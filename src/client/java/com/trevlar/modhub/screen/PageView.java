package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.Checkbox;
import com.trevlar.menukit.api.element.ColorField;
import com.trevlar.menukit.api.element.Dropdown;
import com.trevlar.menukit.api.element.ItemPicker;
import com.trevlar.menukit.api.element.ListView;
import com.trevlar.menukit.api.element.PanelElement;
import com.trevlar.menukit.api.element.Section;
import com.trevlar.menukit.api.element.Slider;
import com.trevlar.menukit.api.element.TextField;
import com.trevlar.menukit.api.element.TextRole;
import com.trevlar.menukit.api.element.WidgetHost;
import com.trevlar.modhub.page.ConfigPage;
import com.trevlar.modhub.page.Control;
import com.trevlar.modhub.page.Value;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Draws a {@link ConfigPage}'s rows into a {@link Body}: the one place a control kind becomes
 * MenuKit elements, so a toggle looks the same on every mod's page whichever library made it.
 *
 * <p>Inventory Plus's layout: a checkbox sits inline with its name; a slider, dropdown or field
 * sits under its name. A row's description is its name's tooltip, as IP's keys' are, and its note
 * is a muted line under the control.
 */
final class PageView {

    private static final int SLIDER_WIDTH = 180;
    private static final int FIELD_WIDTH = 140;
    private static final int DROPDOWN_WIDTH = 110;

    private PageView() {}

    /**
     * Every category's blocks, in order. {@code headed} gives each named category a bold heading;
     * off, when each category is its own tab and the tab already names it.
     */
    static void draw(Body body, List<ConfigPage.Category> categories, boolean headed, History history) {
        for (ConfigPage.Category c : categories) {
            if (headed && c.name() != null) {
                body.gap(4).text(c.name().copy().withStyle(ChatFormatting.BOLD), TextRole.HEADING);
            }
            for (ConfigPage.Block b : c.blocks()) {
                switch (b) {
                    case ConfigPage.Row r -> row(body, r, history);
                    case ConfigPage.Group g -> group(body, g, history);
                }
            }
        }
    }

    /**
     * A group as a MenuKit section: a header that opens and closes its rows. Laid out as if closed;
     * open, the section pushes everything below it down by its own content.
     */
    private static void group(Body body, ConfigPage.Group g, History history) {
        Body inner = new Body();
        for (ConfigPage.Row r : g.rows()) row(inner, r, history);
        body.row((y, v) -> {
            Section.Builder s = Section.builder(g.name()).open(g.startOpen()).content(inner.build());
            return List.of(s.at(0, y).visibleWhen(v).build());
        }, Section.HEADER_HEIGHT + Body.GAP);
    }

    private static void row(Body body, ConfigPage.Row r, History history) {
        Component name = r.name();
        Component tip = r.description();
        Control c = watched(r.control(), r.name(), history);
        switch (c) {
            case Control.Toggle t -> body.row((y, v) -> {
                Checkbox.Builder b = Checkbox.builder().at(0, y).label(name)
                        .state(t.value()::get, t.value()::set).disabledWhen(unavailable(t.value()));
                if (tip != null) b.tooltip(tip);
                return List.of(b.build());
            }, Body.TEXT_ROW + 4);
            // The library's widget draws its own name, as in the library's own screen.
            case Control.Hosted<?> h -> body.row((y, v) -> List.of(hosted(h, y, tip)), h.height() + 4);
            case Control.Action a -> body.row((y, v) -> {
                Button.Builder b = Button.builder().at(0, y).size(0, 16).label(a.label()).onClick(a.onPress());
                if (tip != null) b.tooltip(tip);
                return List.of(b.build());
            }, Body.CONTROL_ROW);
            case Control.Label l -> body.text(name, TextRole.BODY, tip);
            // Never reached: a page with one of these isn't drawn (ConfigPage.drawable), the mod's
            // own screen opens instead. Kept as a line so a slip shows rather than vanishes.
            case Control.Unsupported u -> body.text(Component.literal(name.getString() + " (" + u.what() + ")"), TextRole.MUTED);
            // A list grows as rows are added; MenuKit's reflow pushes what's below down by its height.
            case Control.ListOf<?> l -> {
                body.text(name, TextRole.BODY, tip);
                body.row((y, v) -> List.of(list(l, name, y)), Body.GAP);
            }
            // The rest sit under their name.
            default -> {
                body.text(name, TextRole.BODY, tip);
                body.row((y, v) -> List.of(field(c, name, y)), c instanceof Control.Item ? ItemPicker.SIZE + 4 : Body.CONTROL_ROW);
            }
        }
        if (r.note() != null) body.text(r.note(), TextRole.MUTED);
    }

    /**
     * A control's own element, at {@code y}: what sits under its name on the page, or alone in a
     * list's row. For the kinds that edit inline (a toggle here is a bare checkbox, for a list row).
     */
    private static PanelElement field(Control c, Component name, int y) {
        return switch (c) {
            case Control.Toggle t -> Checkbox.builder().at(0, y)
                    .state(t.value()::get, t.value()::set).disabledWhen(unavailable(t.value())).build();
            case Control.IntSlider s -> Slider.ofInts(s.min(), s.max()).at(0, y).size(SLIDER_WIDTH, 16)
                    .state(s.value()::get, n -> s.value().set(snap(n, s.min(), s.step())))
                    .label(s.format())
                    .disabledWhen(unavailable(s.value())).build();
            case Control.DecimalSlider s -> decimal(s).at(0, y).size(SLIDER_WIDTH, 16)
                    .disabledWhen(unavailable(s.value())).build();
            case Control.NumberField<?> f -> numberField(f).at(0, y).size(FIELD_WIDTH, 16)
                    .disabledWhen(unavailable(f.value())).build();
            case Control.Choice<?> ch -> choice(ch, y);
            case Control.Text t -> drafted(t.value(), t.accepts(), s -> s, s -> s).at(0, y)
                    .size(FIELD_WIDTH, 16).label(name)
                    .disabledWhen(unavailable(t.value())).build();
            case Control.Color col -> ColorField.builder().at(0, y).size(40, 16).label(name).alpha(col.alpha())
                    .state(() -> col.value().get(), n -> col.value().set(n))
                    .disabledWhen(unavailable(col.value())).build();
            case Control.Item i -> ItemPicker.builder().at(0, y).label(name)
                    .state(i.value()::get, i.value()::set)
                    .disabledWhen(unavailable(i.value())).build();
            default -> throw new IllegalArgumentException("not an inline control: " + c);
        };
    }

    /**
     * A list option as a MenuKit list: a row per element, its control with up, down and a remove
     * cross beside it, and "+ Add" above. Elements can repeat, so rows are keyed by an id handed
     * out here for as long as the screen is open, kept in step with the list's order: a row edits
     * the element at its id's place. A change made elsewhere (Reset to Defaults, undo) that changes
     * the length gives the rows at the end new ids; rows read their value by place, so they show
     * it whatever their id.
     */
    private static <T> PanelElement list(Control.ListOf<T> l, Component name, int y) {
        Value<List<T>> list = l.value();
        List<Long> ids = new ArrayList<>();
        long[] next = {0};
        Supplier<List<Long>> items = () -> {
            int size = list.get().size();
            while (ids.size() < size) ids.add(next[0]++);
            while (ids.size() > size) ids.removeLast();
            return List.copyOf(ids);
        };
        return ListView.<Long>builder()
                .items(items).key((Long id) -> id).at(0, y)
                .row(id -> field(l.element().apply(element(list, ids, id, l.newElement().get())), name, 0))
                .add(Component.literal("+ Add"), () -> {
                    List<T> now = new ArrayList<>(list.get());
                    now.add(l.newElement().get());
                    list.set(now);
                })
                .addDisabledWhen(() -> list.get().size() >= l.max())
                // ponytail: at the minimum length the cross does nothing; a greyed cross needs a removeDisabledWhen from MenuKit.
                .remove(id -> {
                    int at = ids.indexOf(id);
                    if (at < 0 || list.get().size() <= l.min()) return;
                    List<T> now = new ArrayList<>(list.get());
                    now.remove(at);
                    ids.remove(at);
                    list.set(now);
                })
                .move((id, to) -> {
                    int from = ids.indexOf(id);
                    if (from < 0) return;
                    List<T> now = new ArrayList<>(list.get());
                    now.add(to, now.remove(from));
                    ids.add(to, ids.remove(from));
                    list.set(now);
                })
                .controlsInRows()
                .disabledWhen(unavailable(list))
                .build();
    }

    /**
     * One element of a list as a value: the element at the id's place; set puts a new list with it
     * replaced. Its default is a new element's.
     */
    private static <T> Value<T> element(Value<List<T>> list, List<Long> ids, long id, T fresh) {
        java.util.function.IntSupplier at = () -> ids.indexOf(id);
        return Value.of(() -> {
            int i = at.getAsInt();
            List<T> now = list.get();
            return i >= 0 && i < now.size() ? now.get(i) : fresh;   // a row on its way out
        }, v -> {
            int i = at.getAsInt();
            List<T> now = new ArrayList<>(list.get());
            if (i < 0 || i >= now.size()) return;
            now.set(i, v);
            list.set(now);
        }, fresh, list::available);
    }

    /** The control with its value recorded by the tab's undo history; the same control otherwise. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Control watched(Control c, Component name, History h) {
        return switch (c) {
            case Control.Toggle t -> new Control.Toggle(h.watch(t.value(), name), t.stateLabel());
            case Control.IntSlider s -> new Control.IntSlider(h.watch(s.value(), name), s.min(), s.max(), s.step(), s.format());
            case Control.DecimalSlider s -> new Control.DecimalSlider(h.watch(s.value(), name), s.min(), s.max(), s.step(), s.format());
            case Control.NumberField f -> new Control.NumberField(h.watch(f.value(), name), f.parse(), f.format());
            case Control.Choice ch -> new Control.Choice(h.watch(ch.value(), name), ch.options(), ch.label());
            case Control.Text t -> new Control.Text(h.watch(t.value(), name), t.accepts());
            case Control.Color col -> new Control.Color(h.watch(col.value(), name), col.alpha());
            case Control.Item i -> new Control.Item(h.watch(i.value(), name));
            // Watched as a copy, so the history's before and after aren't the same live list.
            case Control.ListOf lo -> new Control.ListOf(h.watch(copying(lo.value()), name), lo.element(), lo.newElement(), lo.min(), lo.max());
            case Control.Hosted ho -> ho;
            case Control.Action a -> a;
            case Control.Label l -> l;
            case Control.Unsupported u -> u;
        };
    }

    /** The value a control edits, or null for one that edits nothing. */
    static @Nullable Value<?> valueOf(Control c) {
        return switch (c) {
            case Control.Toggle t -> t.value();
            case Control.IntSlider s -> s.value();
            case Control.DecimalSlider s -> s.value();
            case Control.NumberField<?> f -> f.value();
            case Control.Choice<?> ch -> ch.value();
            case Control.Text t -> t.value();
            case Control.Color col -> col.value();
            case Control.Item i -> i.value();
            case Control.ListOf<?> lo -> lo.value();
            case Control.Hosted<?> ho -> null;
            case Control.Action a -> null;
            case Control.Label l -> null;
            case Control.Unsupported u -> null;
        };
    }

    /** A library's widget in a MenuKit host, at its row. */
    private static <W extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable>
    PanelElement hosted(Control.Hosted<W> h, int y, @Nullable Component tip) {
        WidgetHost.Builder<W> b = WidgetHost.builder().widget(h.widget()).size(h.width(), h.height())
                .place(h.place()::place);
        if (h.overflow()) b.overflow();
        if (tip != null) b.tooltip(tip);
        return b.at(0, y).build();
    }

    private static <T> PanelElement choice(Control.Choice<T> c, int y) {
        return Dropdown.<T>builder().at(0, y).size(DROPDOWN_WIDTH, 16)
                .items(c.options())
                .label(c.label())
                .state(c.value()::get, c.value()::set)
                .disabledWhen(unavailable(c.value()))
                .build();
    }

    /** A list value whose get hands out a copy. */
    private static <T> Value<List<T>> copying(Value<List<T>> v) {
        return Value.of(() -> new ArrayList<>(v.get()), v::set, v.defaultValue(), v::available);
    }

    /** A slider over the fraction 0..1, read and written through the decimal's range and step. */
    private static Slider.Builder<Double> decimal(Control.DecimalSlider s) {
        double span = s.max() - s.min();
        Function<Double, Double> toValue = f -> {
            double raw = s.min() + f * span;
            return s.step() > 0 ? Math.min(s.max(), s.min() + Math.round((raw - s.min()) / s.step()) * s.step()) : raw;
        };
        return Slider.builder()
                .state(() -> span <= 0 ? 0.0 : (s.value().get() - s.min()) / span, f -> s.value().set(toValue.apply(f)))
                .label(f -> s.format().apply(toValue.apply(f)));
    }

    /** A number field: the text in a draft, and only text that parses reaches the value. */
    private static <N extends Number> TextField.Builder numberField(Control.NumberField<N> f) {
        return drafted(f.value(), t -> f.parse().apply(t.strip()) != null, t -> f.parse().apply(t.strip()), f.format());
    }

    /**
     * A text field that keeps what's being typed in a draft, since text like "1." or "minecraft:"
     * is on the way to a valid value without being one: only text {@code accepts} takes reaches the
     * value, through {@code parse}. When the value changes from elsewhere (Reset to Defaults), the
     * draft follows it.
     */
    private static <T> TextField.Builder drafted(Value<T> value, java.util.function.Predicate<String> accepts,
                                                  Function<String, T> parse, Function<T, String> format) {
        Object[] lastSeen = {value.get()};
        String[] draft = {format.apply(value.get())};
        return TextField.builder()
                .state(() -> {
                    if (!Objects.equals(lastSeen[0], value.get())) {
                        lastSeen[0] = value.get();
                        draft[0] = format.apply(value.get());
                    }
                    return draft[0];
                }, text -> {
                    draft[0] = text;
                    if (accepts.test(text)) {
                        value.set(parse.apply(text));
                        lastSeen[0] = value.get();
                    }
                });
    }

    /** {@code n} moved to the nearest step counted from {@code min}. */
    private static int snap(int n, int min, int step) {
        return step <= 1 ? n : min + Math.round((n - min) / (float) step) * step;
    }

    private static BooleanSupplier unavailable(Value<?> value) {
        return () -> !value.available();
    }
}

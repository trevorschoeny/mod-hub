package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.Checkbox;
import com.trevlar.menukit.api.element.ColorField;
import com.trevlar.menukit.api.element.Dropdown;
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

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

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
        switch (watched(r.control(), r.name(), history)) {
            case Control.Toggle t -> body.row((y, v) -> {
                Checkbox.Builder c = Checkbox.builder().at(0, y).label(name)
                        .state(t.value()::get, t.value()::set).disabledWhen(unavailable(t.value()));
                if (tip != null) c.tooltip(tip);
                return List.of(c.build());
            }, Body.TEXT_ROW + 4);
            case Control.IntSlider s -> {
                body.text(name, TextRole.BODY, tip);
                body.row((y, v) -> List.of(Slider.ofInts(s.min(), s.max()).at(0, y).size(SLIDER_WIDTH, 16)
                        .state(s.value()::get, n -> s.value().set(snap(n, s.min(), s.step())))
                        .label(s.format())
                        .disabledWhen(unavailable(s.value())).build()), Body.CONTROL_ROW);
            }
            case Control.DecimalSlider s -> {
                body.text(name, TextRole.BODY, tip);
                body.row((y, v) -> List.of(decimal(s).at(0, y).size(SLIDER_WIDTH, 16)
                        .disabledWhen(unavailable(s.value())).build()), Body.CONTROL_ROW);
            }
            case Control.NumberField<?> f -> {
                body.text(name, TextRole.BODY, tip);
                body.row((y, v) -> List.of(numberField(f).at(0, y).size(FIELD_WIDTH, 16)
                        .disabledWhen(unavailable(f.value())).build()), Body.CONTROL_ROW);
            }
            case Control.Choice<?> c -> choice(body, name, tip, c);
            case Control.Text t -> {
                body.text(name, TextRole.BODY, tip);
                body.row((y, v) -> List.of(drafted(t.value(), t.accepts(), s -> s, s -> s).at(0, y)
                        .size(FIELD_WIDTH, 16).label(name)
                        .disabledWhen(unavailable(t.value())).build()), Body.CONTROL_ROW);
            }
            case Control.Color c -> {
                body.text(name, TextRole.BODY, tip);
                body.row((y, v) -> List.of(ColorField.builder().at(0, y).size(40, 16).label(name).alpha(c.alpha())
                        .state(() -> c.value().get(), n -> c.value().set(n))
                        .disabledWhen(unavailable(c.value())).build()), Body.CONTROL_ROW);
            }
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
        }
        if (r.note() != null) body.text(r.note(), TextRole.MUTED);
    }

    /** The control with its value recorded by the tab's undo history; the same control otherwise. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Control watched(Control c, Component name, History h) {
        return switch (c) {
            case Control.Toggle t -> new Control.Toggle(h.watch(t.value(), name));
            case Control.IntSlider s -> new Control.IntSlider(h.watch(s.value(), name), s.min(), s.max(), s.step(), s.format());
            case Control.DecimalSlider s -> new Control.DecimalSlider(h.watch(s.value(), name), s.min(), s.max(), s.step(), s.format());
            case Control.NumberField f -> new Control.NumberField(h.watch(f.value(), name), f.parse(), f.format());
            case Control.Choice ch -> new Control.Choice(h.watch(ch.value(), name), ch.options(), ch.label());
            case Control.Text t -> new Control.Text(h.watch(t.value(), name), t.accepts());
            case Control.Color col -> new Control.Color(h.watch(col.value(), name), col.alpha());
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

    private static <T> void choice(Body body, Component name, @Nullable Component tip, Control.Choice<T> c) {
        body.text(name, TextRole.BODY, tip);
        body.row((y, v) -> List.of(Dropdown.<T>builder().at(0, y).size(DROPDOWN_WIDTH, 16)
                .items(c.options())
                .label(c.label())
                .state(c.value()::get, c.value()::set)
                .disabledWhen(unavailable(c.value()))
                .build()), Body.CONTROL_ROW);
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

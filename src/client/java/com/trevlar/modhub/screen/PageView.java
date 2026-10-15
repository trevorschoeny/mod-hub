package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.Checkbox;
import com.trevlar.menukit.api.element.Dropdown;
import com.trevlar.menukit.api.element.PanelElement;
import com.trevlar.menukit.api.element.Section;
import com.trevlar.menukit.api.element.Slider;
import com.trevlar.menukit.api.element.TextField;
import com.trevlar.menukit.api.element.TextRole;
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
    static void draw(Body body, List<ConfigPage.Category> categories, boolean headed) {
        for (ConfigPage.Category c : categories) {
            if (headed && c.name() != null) {
                body.gap(4).text(c.name().copy().withStyle(ChatFormatting.BOLD), TextRole.HEADING);
            }
            for (ConfigPage.Block b : c.blocks()) {
                switch (b) {
                    case ConfigPage.Row r -> row(body, r);
                    case ConfigPage.Group g -> group(body, g);
                }
            }
        }
    }

    /**
     * A group as a MenuKit section: a header that opens and closes its rows. Laid out as if closed;
     * open, the section pushes everything below it down by its own content.
     */
    private static void group(Body body, ConfigPage.Group g) {
        Body inner = new Body();
        for (ConfigPage.Row r : g.rows()) row(inner, r);
        body.row((y, v) -> {
            Section.Builder s = Section.builder(g.name()).open(g.startOpen()).content(inner.build());
            return List.of(s.at(0, y).visibleWhen(v).build());
        }, Section.HEADER_HEIGHT + Body.GAP);
    }

    private static void row(Body body, ConfigPage.Row r) {
        Component name = r.name();
        Component tip = r.description();
        switch (r.control()) {
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
                body.row((y, v) -> List.of(TextField.builder().at(0, y).size(FIELD_WIDTH, 16).label(name)
                        .state(t.value()::get, t.value()::set)
                        .disabledWhen(unavailable(t.value())).build()), Body.CONTROL_ROW);
            }
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

    /**
     * A number field keeps what's being typed in a draft, since text like "1." or "-" is on the
     * way to a number without being one: only text that parses reaches the value. When the value
     * changes from elsewhere (Reset to Defaults), the draft follows it.
     */
    private static <N extends Number> TextField.Builder numberField(Control.NumberField<N> f) {
        Value<N> value = f.value();
        Object[] lastSeen = {value.get()};
        String[] draft = {f.format().apply(value.get())};
        return TextField.builder()
                .state(() -> {
                    if (!Objects.equals(lastSeen[0], value.get())) {
                        lastSeen[0] = value.get();
                        draft[0] = f.format().apply(value.get());
                    }
                    return draft[0];
                }, text -> {
                    draft[0] = text;
                    N parsed = f.parse().apply(text.strip());
                    if (parsed != null) {
                        value.set(parsed);
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

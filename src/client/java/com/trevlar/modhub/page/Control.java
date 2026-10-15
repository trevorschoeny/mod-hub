package com.trevlar.modhub.page;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Function;

/**
 * What a row shows, and so how the screen draws it. Every kind a config library offers maps to one
 * of these or to {@link Unsupported}; the screen has one way to draw each.
 */
public sealed interface Control {

    /** On or off. A checkbox inline with the row's name. */
    record Toggle(Value<Boolean> value) implements Control {}

    /** A whole number in a range, as a slider. {@code step} 1 for every number. */
    record IntSlider(Value<Integer> value, int min, int max, int step) implements Control {}

    /**
     * A decimal in a range, as a slider. {@code step} 0 for continuous; {@code format} writes the
     * value inside the track.
     */
    record DecimalSlider(Value<Double> value, double min, double max, double step,
                         Function<Double, Component> format) implements Control {}

    /**
     * A number typed in, with no slider. {@code parse} returns null for text that isn't a valid
     * value, which leaves the value unchanged; {@code format} writes it back as text.
     */
    record NumberField<N extends Number>(Value<N> value, Function<String, N> parse, Function<N, String> format)
            implements Control {}

    /** One of a fixed list, as a dropdown. */
    record Choice<T>(Value<T> value, List<T> options, Function<T, Component> label) implements Control {}

    /** Free text. */
    record Text(Value<String> value) implements Control {}

    /** A button that runs something (YACL's ButtonOption, Cloth's link and action entries). */
    record Action(Component label, Runnable onPress) implements Control {}

    /** Text only, nothing to edit (YACL's LabelOption, Cloth's text entries). */
    record Label() implements Control {}

    /**
     * Something Mod Hub can't draw yet: a colour (until MenuKit has a colour control), a list, or a
     * library's custom widget. Any one of these on a page sends the whole mod to its original screen,
     * so nothing goes missing without saying.
     */
    record Unsupported(String what) implements Control {}
}

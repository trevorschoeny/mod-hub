package com.trevlar.modhub.page;

import net.minecraft.network.chat.Component;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * What a row shows, and so how the screen draws it. Every kind a config library offers maps to one
 * of these or to {@link Unsupported}; the screen has one way to draw each.
 */
public sealed interface Control {

    /** On or off. A checkbox inline with the row's name. */
    record Toggle(Value<Boolean> value) implements Control {}

    /**
     * A whole number in a range, as a slider. {@code step} 1 for every number; {@code format}
     * writes the value inside the track (a library's own wording, such as "50%").
     */
    record IntSlider(Value<Integer> value, int min, int max, int step, Function<Integer, Component> format)
            implements Control {
        /** Shown as the plain number. */
        public IntSlider(Value<Integer> value, int min, int max, int step) {
            this(value, min, max, step, n -> Component.literal(String.valueOf(n)));
        }
    }

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

    /**
     * Free text. {@code accepts} says which text may reach the value: text on its way to valid
     * (an identifier half typed) stays in the field without being written.
     */
    record Text(Value<String> value, Predicate<String> accepts) implements Control {
        /** Any text at all. */
        public Text(Value<String> value) { this(value, s -> true); }
    }

    /**
     * A colour, {@code 0xRRGGBB}, or {@code 0xAARRGGBB} with {@code alpha}. A swatch that opens a
     * colour picker.
     */
    record Color(Value<Integer> value, boolean alpha) implements Control {}

    /** One item, the kind only ({@code Items.AIR} for none). A slot that opens MenuKit's item picker. */
    record Item(Value<net.minecraft.world.item.Item> value) implements Control {}

    /**
     * A library's own widget, drawn as it is in its row: a mod's custom YACL controller, or one
     * Mod Hub has no control of its own for. {@code widget} makes it once; {@code place} moves it to
     * its rectangle each frame (the widget only knows screen coordinates). {@code overflow} for a
     * widget that opens a popup past its row, such as a dropdown's list. It edits its setting
     * itself, so the adapter is what saves the change.
     */
    record Hosted<W extends GuiEventListener & Renderable>(Supplier<W> widget, Place<W> place, int width, int height,
                                                            boolean overflow) implements Control {}

    /** Puts a hosted widget at {@code x, y}, {@code width} by {@code height}, in screen coordinates. */
    @FunctionalInterface
    interface Place<W> {
        void place(W widget, int x, int y, int width, int height);
    }

    /** A button that runs something (YACL's ButtonOption, Cloth's link and action entries). */
    record Action(Component label, Runnable onPress) implements Control {}

    /** Text only, nothing to edit (YACL's LabelOption, Cloth's text entries). */
    record Label() implements Control {}

    /**
     * Something Mod Hub can't draw: a list, or a widget it can't host. Any one of these on a page
     * sends the whole mod to its original screen, so nothing goes missing without saying.
     */
    record Unsupported(String what) implements Control {}
}

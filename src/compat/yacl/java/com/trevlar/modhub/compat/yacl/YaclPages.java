package com.trevlar.modhub.compat.yacl;

import com.trevlar.modhub.page.ConfigPage;
import com.trevlar.modhub.page.Control;
import com.trevlar.modhub.page.Value;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.ListOptionEntry;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.OptionFlag;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ValueFormatter;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.BooleanController;
import dev.isxander.yacl3.gui.controllers.ColorController;
import dev.isxander.yacl3.gui.controllers.LabelController;
import dev.isxander.yacl3.gui.controllers.TickBoxController;
import dev.isxander.yacl3.gui.controllers.cycling.CyclingListController;
import dev.isxander.yacl3.gui.controllers.dropdown.DropdownStringController;
import dev.isxander.yacl3.gui.controllers.dropdown.EnumDropdownController;
import dev.isxander.yacl3.gui.controllers.dropdown.ItemController;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownController;
import dev.isxander.yacl3.gui.controllers.slider.DoubleSliderController;
import dev.isxander.yacl3.gui.controllers.slider.FloatSliderController;
import dev.isxander.yacl3.gui.controllers.slider.IntegerSliderController;
import dev.isxander.yacl3.gui.controllers.slider.LongSliderController;
import dev.isxander.yacl3.gui.controllers.string.StringController;
import dev.isxander.yacl3.gui.controllers.string.number.DoubleFieldController;
import dev.isxander.yacl3.gui.controllers.string.number.FloatFieldController;
import dev.isxander.yacl3.gui.controllers.string.number.IntegerFieldController;
import dev.isxander.yacl3.gui.controllers.string.number.LongFieldController;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Reads a {@code YACLScreen}'s config (its public {@code config}: categories, groups, options) into
 * a {@link ConfigPage}. The original screen is never shown.
 *
 * <p><b>Values save at once.</b> YACL holds a pending value per option and applies them all on its
 * Save button. Mod Hub has no Save button, so each change is applied the moment it's made: the
 * pending value is set and applied (which calls the mod's binding), then the config's save function
 * runs, then the option's flags, as YACL's own Save would. A flag asking for a game restart is shown
 * as a note on the row instead of a prompt on every change.
 *
 * <p>Controllers map onto Mod Hub's controls by class, a colour onto Mod Hub's colour picker, an item onto MenuKit's item picker.
 * Anything else (a mod's own controller) is YACL's own widget, hosted in its
 * row. A list's elements are drawn with the same controls; a list of anything else sends the mod
 * to YACL's screen.
 */
final class YaclPages {

    private static final Component RESTART = Component.literal("Needs a restart to take effect.");

    private YaclPages() {}

    static @Nullable ConfigPage translate(Screen screen) {
        if (!(screen instanceof YACLScreen yacl)) return null;
        YetAnotherConfigLib config = yacl.config;
        List<Option<?>> all = new ArrayList<>();
        List<ConfigPage.Category> categories = new ArrayList<>();
        for (ConfigCategory c : config.categories()) {
            List<ConfigPage.Block> blocks = new ArrayList<>();
            for (OptionGroup g : c.groups()) {
                // A list is a group in YACL (its entries are its options), and one row here.
                if (g instanceof ListOption<?> list) {
                    all.add(list);
                    blocks.add(row(yacl, config, list));
                    continue;
                }
                List<ConfigPage.Row> rows = new ArrayList<>();
                for (Option<?> o : g.options()) {
                    all.add(o);
                    rows.add(row(yacl, config, o));
                }
                // The root group is the category's own options, with no header.
                if (g.isRoot()) blocks.addAll(rows);
                else blocks.add(new ConfigPage.Group(g.name(), text(g.description().text()), !g.collapsed(), rows));
            }
            categories.add(new ConfigPage.Category(c.name(), blocks));
        }
        return new ConfigPage(categories, () -> resetAll(config, all));
    }

    /** Reset to Defaults: every option back to its default, applied, then one save. */
    private static void resetAll(YetAnotherConfigLib config, List<Option<?>> all) {
        for (Option<?> o : all) {
            if (o instanceof ButtonOption || !o.available()) continue;
            o.requestSetDefault();
            o.applyValue();
        }
        config.saveFunction().run();
    }

    private static ConfigPage.Row row(YACLScreen screen, YetAnotherConfigLib config, Option<?> o) {
        Component note = o.flags().contains(OptionFlag.GAME_RESTART) ? RESTART : null;
        return new ConfigPage.Row(o.name(), text(o.description().text()), note, control(screen, config, o));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Control control(YACLScreen screen, YetAnotherConfigLib config, Option<?> o) {
        if (o instanceof ButtonOption b) {
            return new Control.Action(b.name(), () -> b.action().accept(screen, b));
        }
        if (o instanceof ListOption<?> list) return list(config, (ListOption<Object>) list);
        Controller<?> c = o.controller();
        Control k = kind(c, value((Option<Object>) o, config));
        // Anything else (a mod's own controller) is YACL's own widget, hosted in its row.
        return k != null ? k : hosted(screen, config, o, c);
    }

    /**
     * The control for a controller, editing {@code v}: an option's own value, or one element of a
     * list. Null for a controller Mod Hub has no control of its own for.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @Nullable Control kind(Controller<?> c, Value v) {
        return switch (c) {
            case BooleanController x -> new Control.Toggle(v);
            case TickBoxController x -> new Control.Toggle(v);
            case IntegerSliderController s -> new Control.IntSlider(v,
                    (int) s.min(), (int) s.max(), Math.max(1, (int) s.interval()),
                    formatter(s, IntegerSliderController.class, n -> Component.literal(String.valueOf(n))));
            case LongSliderController s -> decimal(as((Value<Long>) v, Number::longValue), s.min(), s.max(), s.interval(),
                    formatter(s, LongSliderController.class, (Long n) -> Component.literal(String.valueOf(n))), Math::round);
            case FloatSliderController s -> decimal(as((Value<Float>) v, Number::floatValue), s.min(), s.max(), s.interval(),
                    formatter(s, FloatSliderController.class, (Float n) -> Component.literal(trim(n))), d -> (float) (double) d);
            case DoubleSliderController s -> decimal(v, s.min(), s.max(), s.interval(),
                    formatter(s, DoubleSliderController.class, (Double n) -> Component.literal(trim(n))), d -> d);
            case IntegerFieldController f -> new Control.NumberField<>((Value<Integer>) v,
                    t -> parse(t, f.min(), f.max(), Integer::parseInt), String::valueOf);
            case LongFieldController f -> new Control.NumberField<>((Value<Long>) v,
                    t -> parse(t, f.min(), f.max(), Long::parseLong), String::valueOf);
            case FloatFieldController f -> new Control.NumberField<>((Value<Float>) v,
                    t -> parse(t, f.min(), f.max(), Float::parseFloat), String::valueOf);
            case DoubleFieldController f -> new Control.NumberField<>((Value<Double>) v,
                    t -> parse(t, f.min(), f.max(), Double::parseDouble), String::valueOf);
            case StringController x -> new Control.Text(v);
            case DropdownStringController d -> d.allowAnyValue
                    ? new Control.Text(v)
                    : new Control.Choice<>((Value<String>) v, d.getAllowedValues(), Component::literal);
            // EnumController is a CyclingListController too, with the enum's constants as its values.
            case CyclingListController<?> x -> cycling(v, x);
            case EnumDropdownController<?> d -> enumDropdown(v, d);
            case LabelController x -> new Control.Label();
            case ColorController x -> colour(v, x.allowAlpha());
            case ItemController x -> new Control.Item(v);
            default -> null;
        };
    }

    /**
     * A list option: each element's control is the one its entry's controller maps to. To learn
     * which, and what a new element starts as, an entry is made as YACL's own add button makes one,
     * and taken out again. Elements whose controller Mod Hub can't draw inline send the mod to
     * YACL's screen.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Control list(YetAnotherConfigLib config, ListOption<Object> list) {
        ListOptionEntry<Object> probe = list.insertNewEntry();
        Object initial = probe.pendingValue();
        Controller<Object> c = probe.controller();
        list.removeEntry(probe);
        Control sample = kind(c, Value.of(() -> initial, x -> {}, initial));
        if (sample == null || sample instanceof Control.Label || sample instanceof Control.Unsupported) return new Control.Unsupported("list of " + c.getClass().getSimpleName());
        int max = list.maximumNumberOfEntries() > 0 ? list.maximumNumberOfEntries() : Integer.MAX_VALUE;
        return new Control.ListOf<>((Value<List<Object>>) (Value) value(list, config), v -> kind(c, v), () -> initial,
                list.minimumNumberOfEntries(), max);
    }

    /** A {@code java.awt.Color} option as a Mod Hub colour: {@code 0xAARRGGBB}, or {@code 0xRRGGBB} without alpha. */
    private static Control colour(Value<Color> v, boolean alpha) {
        return new Control.Color(Value.of(() -> argb(v.get(), alpha), n -> v.set(new Color(alpha ? n : n | 0xFF000000, true)),
                argb(v.defaultValue(), alpha), v::available), alpha);
    }

    private static int argb(Color c, boolean alpha) {
        return alpha ? c.getRGB() : c.getRGB() & 0xFFFFFF;
    }

    /** Width of a hosted YACL widget: it draws the option's name on its left and its control on its right. */
    private static final int HOSTED_WIDTH = 240;

    /**
     * The option's own YACL widget, made by its controller as YACL's screen would make it, against
     * the (never shown) original screen. The widget changes the option's pending value itself, so
     * each change it makes is applied and saved here at once, as every other row's is. A dropdown
     * opens its list past its row, so it's hosted with overflow.
     */
    private static <T> Control hosted(YACLScreen screen, YetAnotherConfigLib config, Option<T> o, Controller<?> c) {
        o.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE && option.changed()) apply(option, config);
        });
        return new Control.Hosted<>(() -> c.provideWidget(screen, Dimension.ofInt(0, 0, HOSTED_WIDTH, 20)),
                (w, x, y, width, height) -> w.setDimension(Dimension.ofInt(x, y, width, height)),
                HOSTED_WIDTH, 20, c instanceof AbstractDropdownController<?>);
    }

    /** Applies the option's pending value, saves, and runs its flags, as YACL's Save would. False when nothing changed. */
    private static boolean apply(Option<?> o, YetAnotherConfigLib config) {
        if (!o.applyValue()) return false;
        config.saveFunction().run();
        Minecraft mc = Minecraft.getInstance();
        for (OptionFlag flag : o.flags()) if (flag != OptionFlag.GAME_RESTART) flag.accept(mc);
        return true;
    }

    /**
     * An option as a Mod Hub value: read the pending value (equal to the mod's own once applied),
     * and on set apply it straight away, save, and run the option's flags.
     */
    private static <T> Value<T> value(Option<T> o, YetAnotherConfigLib config) {
        return Value.of(o::pendingValue, v -> {
            o.requestSet(v);
            apply(o, config);
        }, o.binding().defaultValue(), o::available);
    }

    /** A float or long option as a double value, for the decimal slider. */
    private static <N extends Number> Value<Double> as(Value<N> v, Function<Double, N> back) {
        return Value.of(() -> v.get().doubleValue(), d -> v.set(back.apply(d)), v.defaultValue().doubleValue(), v::available);
    }

    /** A decimal slider whose label is the option's own wording, given the value in its own type. */
    private static <N> Control decimal(Value<Double> v, double min, double max, double step,
                                       Function<N, Component> format, Function<Double, N> asType) {
        return new Control.DecimalSlider(v, min, max, step, d -> format.apply(asType.apply(d)));
    }

    /**
     * The slider's own value wording (its private {@code valueFormatter}, the way YACL writes the
     * value inside the track), or {@code fallback} if a YACL update has renamed it.
     */
    private static <N> Function<N, Component> formatter(Object controller, Class<?> declaring, Function<N, Component> fallback) {
        ValueFormatter<N> f = field(controller, declaring, "valueFormatter");
        return f == null ? fallback : f::format;
    }

    private static <T> Control cycling(Value<T> v, CyclingListController<?> c) {
        // The values and their names are private fields of the controller; read them, and treat
        // the option as unsupported if a YACL update has renamed them.
        List<T> values = field(c, CyclingListController.class, "values");
        ValueFormatter<T> format = field(c, CyclingListController.class, "valueFormatter");
        if (values == null || format == null) return new Control.Unsupported("cycling list");
        return new Control.Choice<>(v, List.copyOf(values), format::format);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Control enumDropdown(Value<Enum<?>> v, EnumDropdownController<?> d) {
        ValueFormatter format = field(d, EnumDropdownController.class, "formatter");
        if (format == null) return new Control.Unsupported("enum dropdown");
        List<Enum<?>> values = Arrays.asList(v.defaultValue().getDeclaringClass().getEnumConstants());
        return new Control.Choice<>(v, values, e -> format.format(e));
    }

    /** A private field of a YACL class, or null if it isn't there. */
    @SuppressWarnings("unchecked")
    private static <T> @Nullable T field(Object of, Class<?> declaring, String name) {
        try {
            Field f = declaring.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(of);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Typed text as a number inside the field's range, or null while it isn't one. */
    private static <N extends Number> @Nullable N parse(String text, double min, double max, Function<String, N> parse) {
        try {
            N n = parse.apply(text);
            return n.doubleValue() < min || n.doubleValue() > max ? null : n;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static @Nullable Component text(Component c) {
        return c == null || c.getString().isBlank() ? null : c;
    }

    private static String trim(double d) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", d);
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }
}

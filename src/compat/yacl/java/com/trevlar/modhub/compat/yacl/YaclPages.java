package com.trevlar.modhub.compat.yacl;

import com.trevlar.modhub.page.ConfigPage;
import com.trevlar.modhub.page.Control;
import com.trevlar.modhub.page.Value;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionFlag;
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
 * <p>Controllers map onto Mod Hub's controls by class. Anything else (a colour, an item, a list, a
 * mod's own controller) is {@link Control.Unsupported}, which sends the mod to YACL's screen.
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

    @SuppressWarnings("unchecked")
    private static Control control(YACLScreen screen, YetAnotherConfigLib config, Option<?> o) {
        if (o instanceof ButtonOption b) {
            return new Control.Action(b.name(), () -> b.action().accept(screen, b));
        }
        if (o instanceof ListOption<?>) return new Control.Unsupported("list");
        Controller<?> c = o.controller();
        return switch (c) {
            case BooleanController x -> new Control.Toggle(value((Option<Boolean>) o, config));
            case TickBoxController x -> new Control.Toggle(value((Option<Boolean>) o, config));
            case IntegerSliderController s -> new Control.IntSlider(value((Option<Integer>) o, config),
                    (int) s.min(), (int) s.max(), Math.max(1, (int) s.interval()));
            case LongSliderController s -> decimal(as((Option<Long>) o, config, Number::longValue), s.min(), s.max(), s.interval(), true);
            case FloatSliderController s -> decimal(as((Option<Float>) o, config, Number::floatValue), s.min(), s.max(), s.interval(), false);
            case DoubleSliderController s -> decimal(value((Option<Double>) o, config), s.min(), s.max(), s.interval(), false);
            case IntegerFieldController f -> new Control.NumberField<>(value((Option<Integer>) o, config),
                    t -> parse(t, f.min(), f.max(), Integer::parseInt), String::valueOf);
            case LongFieldController f -> new Control.NumberField<>(value((Option<Long>) o, config),
                    t -> parse(t, f.min(), f.max(), Long::parseLong), String::valueOf);
            case FloatFieldController f -> new Control.NumberField<>(value((Option<Float>) o, config),
                    t -> parse(t, f.min(), f.max(), Float::parseFloat), String::valueOf);
            case DoubleFieldController f -> new Control.NumberField<>(value((Option<Double>) o, config),
                    t -> parse(t, f.min(), f.max(), Double::parseDouble), String::valueOf);
            case StringController x -> new Control.Text(value((Option<String>) o, config));
            case DropdownStringController d -> d.allowAnyValue
                    ? new Control.Text(value((Option<String>) o, config))
                    : new Control.Choice<>(value((Option<String>) o, config), d.getAllowedValues(), Component::literal);
            // EnumController is a CyclingListController too, with the enum's constants as its values.
            case CyclingListController<?> x -> cycling((Option<Object>) o, config, x);
            case EnumDropdownController<?> d -> enumDropdown((Option<Enum<?>>) o, config, d);
            case LabelController x -> new Control.Label();
            case ColorController x -> new Control.Unsupported("colour");
            case ItemController x -> new Control.Unsupported("item");
            default -> new Control.Unsupported("custom widget " + c.getClass().getSimpleName());
        };
    }

    /**
     * An option as a Mod Hub value: read the pending value (equal to the mod's own once applied),
     * and on set apply it straight away, save, and run the option's flags.
     */
    private static <T> Value<T> value(Option<T> o, YetAnotherConfigLib config) {
        return Value.of(o::pendingValue, v -> {
            o.requestSet(v);
            if (!o.applyValue()) return;   // unchanged: nothing to save
            config.saveFunction().run();
            Minecraft mc = Minecraft.getInstance();
            for (OptionFlag flag : o.flags()) if (flag != OptionFlag.GAME_RESTART) flag.accept(mc);
        }, o.binding().defaultValue(), o::available);
    }

    /** A float or long option as a double value, for the decimal slider. */
    private static <N extends Number> Value<Double> as(Option<N> o, YetAnotherConfigLib config, Function<Double, N> back) {
        Value<N> v = value(o, config);
        return Value.of(() -> v.get().doubleValue(), d -> v.set(back.apply(d)), v.defaultValue().doubleValue(), v::available);
    }

    private static Control decimal(Value<Double> v, double min, double max, double step, boolean whole) {
        return new Control.DecimalSlider(v, min, max, step,
                d -> Component.literal(whole ? String.valueOf(Math.round(d)) : trim(d)));
    }

    private static <T> Control cycling(Option<T> o, YetAnotherConfigLib config, CyclingListController<?> c) {
        // The values and their names are private fields of the controller; read them, and treat
        // the option as unsupported if a YACL update has renamed them.
        List<T> values = field(c, CyclingListController.class, "values");
        ValueFormatter<T> format = field(c, CyclingListController.class, "valueFormatter");
        if (values == null || format == null) return new Control.Unsupported("cycling list");
        return new Control.Choice<>(value(o, config), List.copyOf(values), format::format);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Control enumDropdown(Option<Enum<?>> o, YetAnotherConfigLib config, EnumDropdownController<?> d) {
        ValueFormatter format = field(d, EnumDropdownController.class, "formatter");
        if (format == null) return new Control.Unsupported("enum dropdown");
        List<Enum<?>> values = Arrays.asList(o.binding().defaultValue().getDeclaringClass().getEnumConstants());
        return new Control.Choice<>(value(o, config), values, e -> format.format(e));
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

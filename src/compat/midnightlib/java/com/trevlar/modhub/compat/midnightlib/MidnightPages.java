package com.trevlar.modhub.compat.midnightlib;

import com.trevlar.modhub.page.ConfigPage;
import com.trevlar.modhub.page.Control;
import com.trevlar.modhub.page.Labels;
import com.trevlar.modhub.page.Value;

import eu.midnightdust.lib.config.EntryInfo;
import eu.midnightdust.lib.config.MidnightConfig;
import eu.midnightdust.lib.config.MidnightConfigScreen;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Reads a MidnightLib mod's config (the {@code @Entry} and {@code @Comment} fields MidnightLib
 * registered for it) into a {@link ConfigPage}, from the {@code MidnightConfigScreen} its Mod Menu
 * factory returns. The original screen is never shown or closed (closing it would reload the file
 * over the values).
 *
 * <p>Each field is a public static on the mod's config class. A change sets the field and writes
 * the mod's file at once ({@code MidnightConfig.write}), which is also what calls a mod's own
 * {@code writeChanges} hook.
 *
 * <p>Categories become tabs in the order MidnightLib meets them. A field shown only when another
 * option has some value ({@code @Condition}) is shown greyed out while the condition doesn't hold,
 * rather than appearing and disappearing. Colours, file pickers, lists, and types MidnightLib itself
 * can't edit (long and friends) send the mod to MidnightLib's screen.
 */
final class MidnightPages {

    private MidnightPages() {}

    static @Nullable ConfigPage translate(Screen screen) {
        if (!(screen instanceof MidnightConfigScreen m)) return null;
        String modid = m.modid;
        Map<String, List<ConfigPage.Block>> byCategory = new LinkedHashMap<>();
        List<EntryInfo> editable = new ArrayList<>();
        for (EntryInfo info : Entries.all().values()) {
            if (!info.modid.equals(modid)) continue;
            String category = info.entry != null ? info.entry.category() : info.comment.category();
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(row(modid, info));
            if (info.entry != null) editable.add(info);
        }
        List<ConfigPage.Category> categories = new ArrayList<>();
        byCategory.forEach((cat, rows) -> categories.add(new ConfigPage.Category(categoryName(modid, cat), rows)));
        return new ConfigPage(categories, () -> resetAll(modid, editable));
    }

    /** MidnightLib's protected registry of every mod's fields, keyed "modid:field", in order. */
    private static final class Entries extends MidnightConfig {
        static Map<String, EntryInfo> all() { return entries; }
    }

    private static ConfigPage.Row row(String modid, EntryInfo info) {
        Component name = label(info);
        if (info.entry == null) {
            // A @Comment: a line of text. Centred comments are headings in MidnightLib's own look.
            Component text = info.comment.centered() ? name.copy().withStyle(ChatFormatting.BOLD) : name;
            return new ConfigPage.Row(text, null, null, new Control.Label());
        }
        return new ConfigPage.Row(name, tooltip(info), null, control(modid, info));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Control control(String modid, EntryInfo info) {
        MidnightConfig.Entry e = info.entry;
        if (info.field.getType() == List.class) return new Control.Unsupported("list");
        Class<?> t = info.dataType;
        if (t == boolean.class) return new Control.Toggle(value(modid, info));
        if (t == int.class) {
            int min = (int) e.min(), max = (int) e.max();
            Value<Integer> v = value(modid, info);
            return e.isSlider()
                    ? new Control.IntSlider(v, min, max, 1)
                    : new Control.NumberField<>(v, s -> parse(s, min, max, Integer::parseInt), String::valueOf);
        }
        if (t == float.class) {
            float min = (float) e.min(), max = (float) e.max();
            Value<Float> v = value(modid, info);
            if (!e.isSlider()) return new Control.NumberField<>(v, s -> parse(s, min, max, Float::parseFloat), String::valueOf);
            Value<Double> d = Value.of(() -> (double) v.get(), x -> v.set(round(x, e.precision()).floatValue()),
                    (double) v.defaultValue(), v::available);
            return new Control.DecimalSlider(d, min, max, 1.0 / e.precision(), x -> Component.literal(String.valueOf(round(x, e.precision()))));
        }
        if (t == double.class) {
            double min = e.min(), max = e.max();
            Value<Double> v = value(modid, info);
            return e.isSlider()
                    ? new Control.DecimalSlider(v, min, max, 1.0 / e.precision(), x -> Component.literal(String.valueOf(round(x, e.precision()))))
                    : new Control.NumberField<>(v, s -> parse(s, min, max, Double::parseDouble), String::valueOf);
        }
        if (t == String.class) {
            if (e.isColor()) return new Control.Unsupported("colour");
            if (e.selectionMode() > -1) return new Control.Unsupported("file picker");
            return new Control.Text(value(modid, info), s -> s.length() <= e.width());
        }
        if (t == Identifier.class) {
            Value<Identifier> id = value(modid, info);
            Value<String> asText = Value.of(() -> id.get() == null ? "" : id.get().toString(),
                    s -> id.set(Identifier.tryParse(s)), String.valueOf(id.defaultValue()), id::available);
            return new Control.Text(asText, s -> Identifier.tryParse(s) != null);
        }
        if (t.isEnum()) {
            Value<Enum> v = value(modid, info);
            List<Enum> values = Arrays.asList((Enum[]) t.getEnumConstants());
            return new Control.Choice<>(v, values, en -> enumName(modid, t, en));
        }
        // long, short, byte, char: MidnightLib's own screen has no editor for these either.
        return new Control.Unsupported(t.getSimpleName());
    }

    /**
     * A field as a Mod Hub value: read the static field, set it and write the mod's file. Available
     * while the field's conditions hold.
     */
    @SuppressWarnings("unchecked")
    private static <T> Value<T> value(String modid, EntryInfo info) {
        Field f = info.field;
        return Value.of(() -> {
            try {
                return (T) f.get(null);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }, v -> {
            try {
                if (java.util.Objects.equals(f.get(null), v)) return;
                f.set(null, v);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
            MidnightConfig.write(modid);
        }, (T) MidnightConfig.getDefaultValue(modid, info.fieldName), () -> conditionsMet(modid, info));
    }

    /**
     * MidnightLib's own test: every condition must hold. One names another option (in this mod, or
     * "othermod:option") and the values it must have; a missing option is ignored, as MidnightLib
     * ignores it.
     */
    private static boolean conditionsMet(String modid, EntryInfo info) {
        if (info.conditions == null) return true;
        for (MidnightConfig.Condition c : info.conditions) {
            if (!c.requiredModId().isEmpty() && !FabricLoader.getInstance().isModLoaded(c.requiredModId())) return false;
            if (c.requiredOption().isEmpty()) continue;
            String key = c.requiredOption().contains(":") ? c.requiredOption() : modid + ":" + c.requiredOption();
            EntryInfo target = Entries.all().get(key);
            if (target == null) continue;
            Object current;
            try {
                current = target.field.get(null);
            } catch (IllegalAccessException ex) {
                continue;
            }
            if (current instanceof List<?> list) current = list.isEmpty() ? null : list.getFirst();
            if (!List.of(c.requiredValue()).contains(String.valueOf(current))) return false;
        }
        return true;
    }

    /** Reset to Defaults: every field back to the default MidnightLib kept, then one write. */
    private static void resetAll(String modid, List<EntryInfo> editable) {
        for (EntryInfo info : editable) {
            Object def = MidnightConfig.getDefaultValue(modid, info.fieldName);
            if (def instanceof List<?> l) def = new ArrayList<>(l);   // never hand out the stored default
            try {
                info.field.set(null, def);
            } catch (IllegalAccessException | IllegalArgumentException ignored) {
                // a field MidnightLib couldn't default either; leave it
            }
        }
        MidnightConfig.write(modid);
    }

    /**
     * The field's name: its lang key when the mod has one; otherwise the field name spaced out
     * ("sparkleCount" as "Sparkle count"), rather than the raw key MidnightLib's own screen would show.
     */
    private static Component label(EntryInfo info) {
        if (Language.getInstance().has(info.translationKey)) return Component.translatable(info.translationKey);
        return Component.literal(Labels.humanize(info.fieldName));
    }

    /** The field's tooltip lang key, on its label or its widget, when the mod has one. */
    private static @Nullable Component tooltip(EntryInfo info) {
        for (String key : List.of(info.translationKey + ".label.tooltip", info.translationKey + ".tooltip")) {
            if (Language.getInstance().has(key)) return Component.translatable(key);
        }
        return null;
    }

    /** A category's name: its lang key, or for the default category the mod's config title. */
    private static @Nullable Component categoryName(String modid, String category) {
        String key = modid + ".midnightconfig.category." + category;
        if (Language.getInstance().has(key)) return Component.translatable(key);
        if (category.equals("default")) return null;
        return Component.literal(category.substring(0, 1).toUpperCase(Locale.ROOT) + category.substring(1));
    }

    /** An enum value's name as MidnightLib shows it. */
    private static Component enumName(String modid, Class<?> type, Enum<?> value) {
        if (value instanceof StringRepresentable s) return Component.translatable(s.getSerializedName());
        String key = String.format("%s.midnightconfig.enum.%s.%s", modid, type.getSimpleName(), value);
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(Labels.humanize(value.name().toLowerCase(Locale.ROOT)));
    }

    /** Typed text as a number inside the field's limits, or null while it isn't one. */
    private static <N extends Number> @Nullable N parse(String text, double min, double max, Function<String, N> parse) {
        try {
            N n = parse.apply(text);
            return n.doubleValue() < min || n.doubleValue() > max ? null : n;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** {@code x} rounded to MidnightLib's slider precision (100 means two decimal places). */
    private static Double round(double x, int precision) {
        return precision <= 0 ? x : Math.round(x * precision) / (double) precision;
    }
}

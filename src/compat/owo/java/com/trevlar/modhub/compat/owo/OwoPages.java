package com.trevlar.modhub.compat.owo;

import com.trevlar.modhub.page.ConfigPage;
import com.trevlar.modhub.page.Control;
import com.trevlar.modhub.page.Labels;
import com.trevlar.modhub.page.Value;

import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.ExcludeFromScreen;
import io.wispforest.owo.config.annotation.Expanded;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.RestartRequired;
import io.wispforest.owo.config.annotation.SectionHeader;
import io.wispforest.owo.config.ui.ConfigScreen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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
 * Reads an owo-config wrapper (the {@code ConfigScreen}'s config) into a {@link ConfigPage}: one
 * category, top-level options as rows, a {@code @SectionHeader} as a heading line, and each
 * {@code @Nest}ed object as a group named by its path. The original screen is never shown.
 *
 * <p>owo already owns its values and checks them: a change goes through the option's own
 * constraint, then {@code Option.set} and the wrapper's save. An option the server overrides
 * ({@code detached}) is greyed out, as owo's own screen locks it. Colours, lists and other types owo
 * draws with its own components send the mod to owo's screen.
 *
 * <p><b>Not checked in game yet:</b> written against 0.13.1+26.2's API while owo had no 26.3 build,
 * and switched off until one is checked (see {@link OwoConfigAdapter#testedRange()}).
 */
final class OwoPages {

    private static final Component RESTART = Component.literal("Needs a restart to take effect.");

    private OwoPages() {}

    static @Nullable ConfigPage translate(Screen screen) {
        if (!(screen instanceof ConfigScreen)) return null;
        ConfigWrapper<?> config = field(screen, ConfigScreen.class, "config");
        if (config == null) return null;
        List<ConfigPage.Block> top = new ArrayList<>();
        Map<Option.Key, List<ConfigPage.Row>> nested = new LinkedHashMap<>();
        List<Option<?>> all = new ArrayList<>();
        config.forEachOption(o -> {
            if (o.backingField().hasAnnotation(ExcludeFromScreen.class)) return;
            all.add(o);
            List<ConfigPage.Row> into = o.key().parent().isRoot() ? null : nested.computeIfAbsent(o.key().parent(), k -> new ArrayList<>());
            SectionHeader header = o.backingField().getAnnotation(SectionHeader.class);
            if (header != null) {
                String key = "text.config." + config.name() + ".section." + header.value();
                Component text = (Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(Labels.humanize(header.value())))
                        .copy().withStyle(ChatFormatting.BOLD);
                ConfigPage.Row heading = new ConfigPage.Row(text, null, null, new Control.Label());
                if (into == null) top.add(heading); else into.add(heading);
            }
            ConfigPage.Row row = row(config, o);
            if (into == null) top.add(row); else into.add(row);
        });
        nested.forEach((key, rows) -> {
            String k = "text.config." + config.name() + ".category." + key.asString();
            Component name = Language.getInstance().has(k) ? Component.translatable(k)
                    : Component.literal(String.join(" / ", Arrays.stream(key.asString().split("\\.")).map(Labels::humanize).toList()));
            top.add(new ConfigPage.Group(name, null, expanded(config, key), rows));
        });
        return new ConfigPage(List.of(new ConfigPage.Category(null, top)), () -> resetAll(config, all));
    }

    /** Whether the nested object at {@code key} is marked {@code @Expanded}, so its group starts open. */
    private static boolean expanded(ConfigWrapper<?> config, Option.Key key) {
        try {
            Field f = config.fieldForKey(key);
            return f != null && f.isAnnotationPresent(Expanded.class);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static ConfigPage.Row row(ConfigWrapper<?> config, Option<?> o) {
        String key = o.translationKey();
        Component name = Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(Labels.humanize(o.key().name()));
        Component tip = Language.getInstance().has(key + ".tooltip") ? Component.translatable(key + ".tooltip") : null;
        Component note = o.backingField().hasAnnotation(RestartRequired.class) ? RESTART : null;
        return new ConfigPage.Row(name, tip, note, control(config, o));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Control control(ConfigWrapper<?> config, Option<?> o) {
        Class<?> t = o.clazz();
        RangeConstraint range = o.backingField().getAnnotation(RangeConstraint.class);
        if (t == boolean.class || t == Boolean.class) return new Control.Toggle(value(config, (Option<Boolean>) o));
        if (t == int.class || t == Integer.class) {
            Value<Integer> v = value(config, (Option<Integer>) o);
            if (range != null && range.max() - range.min() <= 1000) return new Control.IntSlider(v, (int) range.min(), (int) range.max(), 1);
            return new Control.NumberField<>(v, s -> parse(s, (Option<Integer>) o, Integer::parseInt), String::valueOf);
        }
        if (t == long.class || t == Long.class) {
            return new Control.NumberField<>(value(config, (Option<Long>) o), s -> parse(s, (Option<Long>) o, Long::parseLong), String::valueOf);
        }
        if (t == double.class || t == Double.class || t == float.class || t == Float.class) {
            boolean isFloat = t == float.class || t == Float.class;
            Value<Number> raw = value(config, (Option<Number>) o);
            Value<Double> v = Value.of(() -> raw.get().doubleValue(),
                    d -> raw.set(isFloat ? (Number) (float) (double) d : d), raw.defaultValue().doubleValue(), raw::available);
            if (range != null) {
                int places = Math.max(0, range.decimalPlaces());
                double step = Math.pow(10, -places);
                return new Control.DecimalSlider(v, range.min(), range.max(), step,
                        d -> Component.literal(String.format(Locale.ROOT, "%." + places + "f", d)));
            }
            return new Control.NumberField<>(v, s -> {
                try {
                    double d = Double.parseDouble(s);
                    return ((Option) o).verifyConstraint(isFloat ? (Object) (float) d : (Object) d) ? d : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            }, String::valueOf);
        }
        if (t == String.class) {
            return new Control.Text(value(config, (Option<String>) o), s -> ((Option<String>) o).verifyConstraint(s));
        }
        if (t == Identifier.class) {
            Value<Identifier> id = value(config, (Option<Identifier>) o);
            Value<String> asText = Value.of(() -> String.valueOf(id.get()), s -> id.set(Identifier.tryParse(s)),
                    String.valueOf(id.defaultValue()), id::available);
            return new Control.Text(asText, s -> Identifier.tryParse(s) != null);
        }
        if (t.isEnum()) {
            List<Enum> values = Arrays.asList((Enum[]) t.getEnumConstants());
            return new Control.Choice<>(value(config, (Option<Enum>) o), values, e -> enumName(config, o, e));
        }
        return new Control.Unsupported(t.getSimpleName());
    }

    /**
     * An option as a Mod Hub value: its value, set through its constraint and saved. Unavailable
     * while the server overrides it.
     */
    private static <T> Value<T> value(ConfigWrapper<?> config, Option<T> o) {
        return Value.of(o::value, v -> {
            if (v == null || v.equals(o.value()) || !o.verifyConstraint(v)) return;
            o.set(v);
            config.save();
        }, o.defaultValue(), () -> !o.detached());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void resetAll(ConfigWrapper<?> config, List<Option<?>> all) {
        for (Option o : all) if (!o.detached()) o.set(o.defaultValue());
        config.save();
    }

    /** An enum value's name: owo's option-specific key, then its enum key, then the value spaced out. */
    private static Component enumName(ConfigWrapper<?> config, Option<?> o, Enum<?> e) {
        String value = e.name().toLowerCase(Locale.ROOT);
        for (String key : List.of(o.translationKey() + ".value." + value,
                "text.config." + config.name() + ".enum." + e.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT) + "." + value)) {
            if (Language.getInstance().has(key)) return Component.translatable(key);
        }
        return Component.literal(Labels.humanize(value));
    }

    /** Typed text as a value the option accepts, or null while it isn't one. */
    private static <N> @Nullable N parse(String text, Option<N> o, Function<String, N> parse) {
        try {
            N n = parse.apply(text);
            return o.verifyConstraint(n) ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nullable T field(Object of, Class<?> declaring, String name) {
        try {
            Field f = declaring.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(of);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }
}

package com.trevlar.modhub.compat.fcap;

import com.trevlar.modhub.page.ConfigPage;
import com.trevlar.modhub.page.Control;
import com.trevlar.modhub.page.Labels;
import com.trevlar.modhub.page.Value;

import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Reads the configs a mod registered with Forge Config API Port into a {@link ConfigPage}, from the
 * NeoForge {@code ConfigurationScreen} its Mod Menu factory returns. The screen itself is never
 * shown; the values come from the mod's {@code ModConfigSpec}s, the way that screen reads them.
 *
 * <p>Each config file (Client, Common, Server, Startup) is a category, and each section in it a
 * group, nested sections named by their path ("Rendering / Fog"). A change is checked against the
 * value's own rule, set, and the file saved at once.
 *
 * <p>A Server config is only loaded while a world is open, and belongs to the server when playing
 * on one: its rows are greyed out then, as NeoForge's own screen has them. Lists send the mod to
 * its own screen.
 */
final class SpecPages {

    private static final Component RESTART_GAME = Component.literal("Needs a restart to take effect.");
    private static final Component RESTART_WORLD = Component.literal("Takes effect when the world is reloaded.");

    private SpecPages() {}

    static @Nullable ConfigPage translate(String modId, Screen screen) {
        if (!(screen instanceof ConfigurationScreen)) return null;
        List<ConfigPage.Category> categories = new ArrayList<>();
        List<Runnable> resets = new ArrayList<>();
        for (ModConfig.Type type : ModConfig.Type.values()) {
            for (ModConfig config : ModConfigs.getModConfigs(modId)) {
                if (config.getType() != type || !(config.getSpec() instanceof ModConfigSpec spec)) continue;
                List<ConfigPage.Block> blocks = new ArrayList<>();
                walk(modId, config, spec, spec.getValues(), spec.getSpec(), List.of(), blocks, resets);
                if (!blocks.isEmpty()) categories.add(new ConfigPage.Category(Component.literal(typeName(type)), blocks));
            }
        }
        return new ConfigPage(categories, () -> resets.forEach(Runnable::run));
    }

    /**
     * One level of a spec: its values become rows in {@code out}, and each section a group of its
     * own after them, so a section's rows sit under its header and nested sections stay readable.
     */
    private static void walk(String modId, ModConfig config, ModConfigSpec spec, UnmodifiableConfig values,
                             UnmodifiableConfig specs, List<String> path, List<ConfigPage.Block> out, List<Runnable> resets) {
        List<ConfigPage.Row> rows = new ArrayList<>();
        List<ConfigPage.Block> sections = new ArrayList<>();
        for (Map.Entry<String, Object> e : values.valueMap().entrySet()) {
            List<String> here = new ArrayList<>(path);
            here.add(e.getKey());
            Object s = specs.valueMap().get(e.getKey());
            if (e.getValue() instanceof UnmodifiableConfig sub && s instanceof UnmodifiableConfig subSpec) {
                List<ConfigPage.Block> inner = new ArrayList<>();
                walk(modId, config, spec, sub, subSpec, here, inner, resets);
                // Groups don't nest: a section's own rows make its group, and sections inside it
                // follow as groups of their own, named by their whole path.
                List<ConfigPage.Row> own = new ArrayList<>();
                for (ConfigPage.Block b : inner) if (b instanceof ConfigPage.Row r) own.add(r);
                Component name = sectionName(modId, spec, here);
                if (!own.isEmpty()) sections.add(new ConfigPage.Group(name, comment(spec.getLevelComment(here)), false, own));
                for (ConfigPage.Block b : inner) if (b instanceof ConfigPage.Group g) sections.add(g);
            } else if (e.getValue() instanceof ModConfigSpec.ConfigValue<?> cv && s instanceof ModConfigSpec.ValueSpec vs) {
                rows.add(row(modId, config, spec, cv, vs, here));
                resets.add(() -> reset(config, spec, cv));
            }
        }
        out.addAll(rows);
        out.addAll(sections);
    }

    private static ConfigPage.Row row(String modId, ModConfig config, ModConfigSpec spec,
                                      ModConfigSpec.ConfigValue<?> cv, ModConfigSpec.ValueSpec vs, List<String> path) {
        String key = translationKey(modId, spec, vs, path);
        Component name = Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(Labels.humanize(path.getLast()));
        Component tip = Language.getInstance().has(key + ".tooltip") ? Component.translatable(key + ".tooltip") : comment(vs.getComment());
        Component note = switch (vs.restartType()) {
            case GAME -> RESTART_GAME;
            case WORLD -> RESTART_WORLD;
            default -> null;
        };
        return new ConfigPage.Row(name, tip, note, control(config, spec, cv, vs));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Control control(ModConfig config, ModConfigSpec spec, ModConfigSpec.ConfigValue<?> cv, ModConfigSpec.ValueSpec vs) {
        if (vs instanceof ModConfigSpec.ListValueSpec) return new Control.Unsupported("list");
        ModConfigSpec.Range<?> range = vs.getRange();
        return switch (cv) {
            case ModConfigSpec.BooleanValue b -> new Control.Toggle(value(config, spec, b, vs));
            case ModConfigSpec.IntValue i -> {
                Value<Integer> v = value(config, spec, i, vs);
                int min = range != null ? (Integer) range.getMin() : Integer.MIN_VALUE;
                int max = range != null ? (Integer) range.getMax() : Integer.MAX_VALUE;
                // A slider only for a range a player could drag through; a wide one is typed.
                yield range != null && (long) max - min <= 1000
                        ? new Control.IntSlider(v, min, max, 1)
                        : new Control.NumberField<>(v, t -> parse(t, vs, Integer::parseInt), String::valueOf);
            }
            case ModConfigSpec.LongValue l -> new Control.NumberField<>(value(config, spec, l, vs), t -> parse(t, vs, Long::parseLong), String::valueOf);
            case ModConfigSpec.DoubleValue d -> new Control.NumberField<>(value(config, spec, d, vs), t -> parse(t, vs, Double::parseDouble), String::valueOf);
            case ModConfigSpec.EnumValue<?> en -> {
                // The allowed values are the enum's constants that pass the value's own rule.
                List<Enum> allowed = Arrays.stream((Enum[]) vs.getClazz().getEnumConstants()).filter(vs::test).toList();
                yield new Control.Choice<>((Value<Enum>) (Value) value(config, spec, en, vs), allowed,
                        e -> e instanceof TranslatableEnum t ? t.getTranslatedName() : Component.literal(e.name()));
            }
            default -> {
                if (vs.getClazz() == String.class || cv.getDefault() instanceof String) {
                    yield new Control.Text(value(config, spec, (ModConfigSpec.ConfigValue<String>) cv, vs), vs::test);
                }
                yield new Control.Unsupported(cv.getDefault() == null ? "value" : cv.getDefault().getClass().getSimpleName());
            }
        };
    }

    /**
     * A spec value as a Mod Hub value. Read from the loaded file ({@code getRaw}, so a change that
     * waits for a restart still shows as made), or the default while the file isn't loaded. Set
     * checks the value's rule, sets it and saves the file.
     */
    private static <T> Value<T> value(ModConfig config, ModConfigSpec spec, ModConfigSpec.ConfigValue<T> cv, ModConfigSpec.ValueSpec vs) {
        return Value.of(() -> spec.isLoaded() ? cv.getRaw() : cv.getDefault(), v -> {
            if (!spec.isLoaded() || !vs.test(v) || v.equals(cv.getRaw())) return;
            cv.set(v);
            spec.save();
        }, cv.getDefault(), () -> editable(config, spec));
    }

    private static void reset(ModConfig config, ModConfigSpec spec, ModConfigSpec.ConfigValue<?> cv) {
        if (!editable(config, spec)) return;
        reset(spec, cv);
    }

    private static <T> void reset(ModConfigSpec spec, ModConfigSpec.ConfigValue<T> cv) {
        cv.set(cv.getDefault());
        spec.save();
    }

    /**
     * Whether the player can change this config now: it must be loaded, and a Server config belongs
     * to the server unless the world is this player's own and not open to LAN.
     */
    private static boolean editable(ModConfig config, ModConfigSpec spec) {
        if (!spec.isLoaded()) return false;
        if (config.getType() != ModConfig.Type.SERVER) return true;
        Minecraft mc = Minecraft.getInstance();
        return mc.getCurrentServer() == null || (mc.hasSingleplayerServer() && !mc.getSingleplayerServer().isPublished());
    }

    /** The value's lang key: its own, else its section's, else NeoForge's "<mod>.configuration.<key>". */
    private static String translationKey(String modId, ModConfigSpec spec, ModConfigSpec.ValueSpec vs, List<String> path) {
        if (vs.getTranslationKey() != null) return vs.getTranslationKey();
        String level = spec.getLevelTranslationKey(path);
        return level != null ? level : modId + ".configuration." + path.getLast();
    }

    /** A section's name: its lang key when the mod has one, else its path, spaced out. */
    private static Component sectionName(String modId, ModConfigSpec spec, List<String> path) {
        String key = spec.getLevelTranslationKey(path);
        if (key == null) key = modId + ".configuration." + path.getLast();
        if (Language.getInstance().has(key)) return Component.translatable(key);
        return Component.literal(String.join(" / ", path.stream().map(Labels::humanize).toList()));
    }

    private static @Nullable Component comment(@Nullable String text) {
        return text == null || text.isBlank() ? null : Component.literal(text.strip());
    }

    private static String typeName(ModConfig.Type type) {
        String n = type.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    /** Typed text as a value the spec accepts, or null while it isn't one. */
    private static <N extends Number> @Nullable N parse(String text, ModConfigSpec.ValueSpec vs, Function<String, N> parse) {
        try {
            N n = parse.apply(text);
            return vs.test(n) ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

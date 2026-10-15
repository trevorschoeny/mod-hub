package com.trevlar.modhub.page;

import com.trevlar.modhub.api.Setting;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A setting's name and description, from its lang keys {@code modhub.<modid>.<key>} and
 * {@code .desc}; a choice's value names from {@code modhub.<modid>.<key>.<value>}. A mod with no
 * lang entry still reads: the key, spaced out ("railFreePlacement" as "Rail free placement").
 */
public final class Labels {

    private Labels() {}

    public static Component name(Setting<?> s) {
        return lang(base(s), humanize(s.key()));
    }

    public static @Nullable Component description(Setting<?> s) {
        String key = base(s) + ".desc";
        return Language.getInstance().has(key) ? Component.translatable(key) : null;
    }

    public static Component value(Setting<?> s, Enum<?> v) {
        return lang(base(s) + "." + v.name().toLowerCase(Locale.ROOT), humanize(v.name().toLowerCase(Locale.ROOT)));
    }

    private static String base(Setting<?> s) {
        return "modhub." + s.owner().modId() + "." + s.key();
    }

    private static Component lang(String key, String fallback) {
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(fallback);
    }

    /** {@code "railFreePlacement"} or {@code "rail_free"} to {@code "Rail free placement"}. */
    public static String humanize(String key) {
        String spaced = key.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').toLowerCase(Locale.ROOT).strip();
        return spaced.isEmpty() ? key : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}

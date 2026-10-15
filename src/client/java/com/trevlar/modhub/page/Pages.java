package com.trevlar.modhub.page;

import com.trevlar.modhub.catalog.ModInfo;

import org.jetbrains.annotations.Nullable;

/**
 * The one place a mod's page comes from: its Mod Hub settings, translated. Null when Mod Hub can't
 * draw the mod's settings itself, and the mod's own screen opens instead.
 */
public final class Pages {

    private Pages() {}

    public static @Nullable ConfigPage of(ModInfo mod) {
        if (mod.settings() != null) return NativePage.of(mod.settings());
        return null;
    }
}

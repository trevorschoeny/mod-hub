package com.trevlar.modhub;

import com.trevlar.modhub.api.ModHubEntry;
import com.trevlar.modhub.api.Setting;
import com.trevlar.modhub.api.Settings;

/**
 * Mod Hub's own settings, on Mod Hub like any other mod's: it's the first entry in the mod list,
 * and its Configure opens these. The {@code modhub} entrypoint in Mod Hub's own manifest.
 */
public final class ModHubSettings implements ModHubEntry {

    public static final Settings S = ModHub.settings(ModHub.MOD_ID);

    /** Mod Hub's screens in the options look (the default) or the inventory look. */
    public static final Setting<Boolean> OPTIONS_LOOK = S.bool("optionsLook", true);

    /**
     * Mods built on another config library (YACL, Cloth Config, MidnightLib, Forge Config API Port)
     * open as Mod Hub pages. Off, each opens its own screen, as from Mod Menu.
     */
    public static final Setting<Boolean> ADAPT_OTHER_LIBRARIES = S.bool("adaptOtherLibraries", true);

    @Override
    public Settings settings() {
        return S;
    }
}

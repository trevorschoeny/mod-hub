package com.trevlar.modhub.prodcheck;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * A {@code modmenu} entrypoint that reaches past Mod Menu's API into its own classes, as a mod
 * might. Without Mod Menu it fails to construct, and Mod Hub should list this mod with no gear
 * and one log line, never crash.
 */
public final class NeedsModMenuEntry implements ModMenuApi {

    private final Class<?> modMenu = ModMenu.class;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> parent;
    }
}

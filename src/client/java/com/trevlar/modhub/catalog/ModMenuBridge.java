package com.trevlar.modhub.catalog;

import com.terraformersmc.modmenu.ModMenu;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

import org.jetbrains.annotations.Nullable;

/**
 * Mod Hub's one door to Mod Menu, for mods whose settings screen comes through Mod Menu's
 * {@code modmenu} entrypoint (YACL and others). Those screens can only be built through Mod Menu's
 * own code, so this asks Mod Menu's public statics and nothing else.
 *
 * <p>Every call is behind {@code isModLoaded("modmenu")}: without Mod Menu this class's Mod Menu
 * references are never resolved, and those mods sit in Auxiliary.
 */
public final class ModMenuBridge {

    private ModMenuBridge() {}

    private static boolean present() {
        return FabricLoader.getInstance().isModLoaded("modmenu");
    }

    /** Whether Mod Menu has a settings screen for {@code modId}. */
    public static boolean hasScreen(String modId) {
        if (!present()) return false;
        try {
            return ModMenu.hasConfigScreen(modId);
        } catch (Throwable t) {
            return false;   // a mod's own factory broke; it's listed without settings
        }
    }

    /** That screen, over {@code parent}, or null. */
    public static @Nullable Screen screen(String modId, Screen parent) {
        return present() ? ModMenu.getConfigScreen(modId, parent) : null;
    }
}

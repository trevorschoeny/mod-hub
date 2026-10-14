package com.trevlar.modhub.catalog;

import com.trevorschoeny.keybindery.api.KeybinderyAPI;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * Mod Hub's one door to Keybindery, through {@code keybindery-api}: its Key Binds screen, opened
 * filtered to one mod, and whether a mod has any keys at all. Behind
 * {@code isModLoaded("keybindery")}, so without Keybindery the API's classes are never touched
 * and the keys button simply isn't there.
 */
public final class KeybinderyBridge {

    /** Keybindery's own button sprite in the pause menu, so the two read as the same thing. */
    public static final Identifier KEYS_SPRITE = Identifier.fromNamespaceAndPath("keybindery", "pause_menu/keys");

    private KeybinderyBridge() {}

    public static boolean present() {
        return FabricLoader.getInstance().isModLoaded("keybindery");
    }

    /** Whether {@code modId} has keys for the button to show. False without Keybindery. */
    public static boolean hasKeybinds(String modId) {
        return present() && KeybinderyAPI.getInstance().hasKeybinds(modId);
    }

    /** Opens Keybindery's Key Binds screen showing every one of these mods' keys together, over {@code current}. */
    public static void open(java.util.List<String> modIds, Screen current) {
        if (present() && !modIds.isEmpty()) KeybinderyAPI.getInstance().openModKeybinds(modIds, current);
    }
}

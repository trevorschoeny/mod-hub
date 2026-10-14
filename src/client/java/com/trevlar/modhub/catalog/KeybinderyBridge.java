package com.trevlar.modhub.catalog;

import com.trevorschoeny.keybindery.screen.KeybinderyKeyBindsScreen;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * Mod Hub's one door to Keybindery: its Key Binds screen, opened filtered to one mod. Behind
 * {@code isModLoaded("keybindery")}, so without Keybindery the class's references are never
 * resolved and the keys button simply isn't there.
 *
 * <p>ponytail: calls Keybindery's own {@code KeybinderyKeyBindsScreen.openWithModFilterFor}, which
 * is public but not in {@code keybindery-api}. Asked the Keybindery agent for it in the API
 * (2026-10-14); switch to that and drop the compile line in build.gradle when it lands.
 */
public final class KeybinderyBridge {

    /** Keybindery's own button sprite in the pause menu, so the two read as the same thing. */
    public static final Identifier KEYS_SPRITE = Identifier.fromNamespaceAndPath("keybindery", "pause_menu/keys");

    private KeybinderyBridge() {}

    public static boolean present() {
        return FabricLoader.getInstance().isModLoaded("keybindery");
    }

    /** Opens Keybindery's Key Binds screen showing {@code modId}'s keys, over {@code current}. */
    public static void open(String modId, Screen current) {
        if (present()) KeybinderyKeyBindsScreen.openWithModFilterFor(modId, current);
    }
}

package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screens.Screen;

/**
 * Mod Hub's stand-in for Mod Menu's interface of the same name, signature for signature, so a
 * mod's {@code modmenu} entrypoint links when Mod Menu isn't installed. With Mod Menu installed
 * this jar is never loaded and Mod Menu's own copy is the only one.
 */
@FunctionalInterface
public interface ConfigScreenFactory<S extends Screen> {
    S create(Screen parent);
}

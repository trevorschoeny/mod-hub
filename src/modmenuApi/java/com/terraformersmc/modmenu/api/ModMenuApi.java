package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Mod Hub's stand-in for Mod Menu's entrypoint interface, with the same methods and defaults as
 * Mod Menu 21, so a mod's {@code modmenu} entrypoint class links and Mod Hub can ask it for its
 * settings screen. Update checkers and badges are accepted and ignored: Mod Hub has neither.
 */
public interface ModMenuApi {

    /**
     * Mod Menu's own list, in Mod Menu. Here Mod Hub's list, looked up by name because this jar is
     * built apart from Mod Hub's classes.
     */
    static Screen createModsScreen(Screen previous) {
        try {
            return (Screen) Class.forName("com.trevlar.modhub.screen.ModListScreen")
                    .getMethod("create", Screen.class).invoke(null, previous);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Mod Hub's mod list is missing", e);
        }
    }

    static Component createModsButtonText() {
        return Component.literal("Mods");
    }

    default ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> null;
    }

    default UpdateChecker getUpdateChecker() {
        return null;
    }

    default Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return Map.of();
    }

    default Map<String, UpdateChecker> getProvidedUpdateCheckers() {
        return Map.of();
    }

    default void attachModpackBadges(Consumer<String> consumer) {}
}

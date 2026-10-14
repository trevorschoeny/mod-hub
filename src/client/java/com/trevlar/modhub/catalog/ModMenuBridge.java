package com.trevlar.modhub.catalog;

import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Mod Hub's one door to the settings screens other mods register through Mod Menu's
 * {@code modmenu} entrypoint (YACL mods, Iris, and most others). Two ways through, never both:
 *
 * <ul>
 *   <li><b>Mod Menu installed:</b> ask Mod Menu's public statics, so a screen opens exactly as
 *       it would from Mod Menu's own list.</li>
 *   <li><b>Mod Menu absent:</b> Mod Hub's jar carries a nested stand-in for Mod Menu's API types
 *       ({@code modhub_modmenu_api}), which the loader drops whenever Mod Menu itself is
 *       installed. With it loaded, the entrypoints link, and this class reads them the way Mod Menu
 *       does: each mod's own factory, then the factories mods provide for others, a mod's own
 *       always winning.</li>
 * </ul>
 *
 * <p>A mod whose entrypoint or screen fails is logged once and treated as having no screen, so it
 * sits in Auxiliary with no gear rather than crashing the list.
 */
public final class ModMenuBridge {

    private static final Logger LOG = LoggerFactory.getLogger("modhub");

    /** The stand-in path's factories by mod id, read once on first use. */
    private static @Nullable Map<String, ConfigScreenFactory<?>> factories;

    private ModMenuBridge() {}

    private static boolean modMenu() {
        return FabricLoader.getInstance().isModLoaded("modmenu");
    }

    private static boolean standIn() {
        return FabricLoader.getInstance().isModLoaded("modhub_modmenu_api");
    }

    /** Whether {@code modId} has a settings screen through Mod Menu's API. */
    public static boolean hasScreen(String modId) {
        if (modMenu()) {
            try {
                return ModMenu.hasConfigScreen(modId);
            } catch (Throwable t) {
                return false;   // a mod's own factory broke; it's listed without settings
            }
        }
        return standIn() && factories().containsKey(modId);
    }

    /** That screen, over {@code parent}, or null. */
    public static @Nullable Screen screen(String modId, Screen parent) {
        if (modMenu()) return ModMenu.getConfigScreen(modId, parent);
        if (!standIn()) return null;
        ConfigScreenFactory<?> factory = factories().get(modId);
        if (factory == null) return null;
        try {
            return factory.create(parent);
        } catch (Throwable t) {
            LOG.warn("Mod Hub: {}'s settings screen failed to open: {}", modId, t.toString());
            return null;
        }
    }

    private static Map<String, ConfigScreenFactory<?>> factories() {
        if (factories != null) return factories;
        Map<String, ConfigScreenFactory<?>> own = new HashMap<>();
        Map<String, ConfigScreenFactory<?>> provided = new HashMap<>();
        // Vanilla's Options screen as Minecraft's settings, as Mod Menu provides it itself.
        provided.put("minecraft", parent -> new OptionsScreen(parent, Minecraft.getInstance().options));
        for (EntrypointContainer<ModMenuApi> c : FabricLoader.getInstance().getEntrypointContainers("modmenu", ModMenuApi.class)) {
            String id = c.getProvider().getMetadata().getId();
            try {
                ModMenuApi api = c.getEntrypoint();
                ConfigScreenFactory<?> factory = api.getModConfigScreenFactory();
                if (factory != null) own.put(id, factory);
                api.getProvidedConfigScreenFactories().forEach(provided::putIfAbsent);
            } catch (Throwable t) {
                // Needs more of Mod Menu than the stand-in has, or broke on its own: no gear.
                LOG.warn("Mod Hub: {}'s modmenu entrypoint couldn't load without Mod Menu: {}", id, t.toString());
            }
        }
        provided.putAll(own);
        return factories = provided;
    }
}

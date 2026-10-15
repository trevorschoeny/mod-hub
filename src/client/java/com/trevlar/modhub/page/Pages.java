package com.trevlar.modhub.page;

import com.trevlar.modhub.ModHubSettings;
import com.trevlar.modhub.catalog.ModInfo;
import com.trevlar.modhub.catalog.ModMenuBridge;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The one place a mod's page comes from:
 * <ol>
 *   <li>a mod on Mod Hub: its settings, translated ({@link NativePage});</li>
 *   <li>a mod with a screen through Mod Menu's entrypoint: that screen, asked for and handed to each
 *       adapter in turn, unless the player has turned adapting off;</li>
 *   <li>otherwise null, and the mod's own screen opens as it would from Mod Menu.</li>
 * </ol>
 *
 * <p>The original screen is built but never shown. A page with anything Mod Hub can't draw counts
 * as no page, so a mod is never shown with settings missing.
 */
public final class Pages {

    private static final Logger LOG = LoggerFactory.getLogger("modhub");

    /** The adapters whose library is installed and in range, found once. */
    private static @Nullable List<Adapter> adapters;

    /**
     * Pages already made since the config screen last opened: a tab is drawn more than once (every
     * relayout), and building a library's screen each time would be wasted work. Cleared by
     * {@link #forget()} when a config screen opens, so each opening reads the mod afresh.
     */
    private static final Map<String, Optional<ConfigPage>> made = new HashMap<>();

    private Pages() {}

    public static @Nullable ConfigPage of(ModInfo mod) {
        return made.computeIfAbsent(mod.id(), id -> Optional.ofNullable(make(mod))).orElse(null);
    }

    /** Called as a config screen opens. */
    public static void forget() {
        made.clear();
    }

    private static @Nullable ConfigPage make(ModInfo mod) {
        if (mod.settings() != null) return NativePage.of(mod.settings());
        if (!mod.modMenuScreen() || !ModHubSettings.ADAPT_OTHER_LIBRARIES.get()) return null;
        Screen original = ModMenuBridge.screen(mod.id(), Minecraft.getInstance().gui.screen());
        if (original == null) return null;
        for (Adapter a : adapters()) {
            try {
                ConfigPage page = a.translate(original);
                if (page == null) continue;   // another library's screen
                if (page.drawable()) return page;
                LOG.info("Mod Hub: {} has settings Mod Hub can't draw yet; it opens its own screen", mod.id());
                return null;
            } catch (Throwable t) {
                LOG.warn("Mod Hub: couldn't read {}'s {} screen; it opens its own: {}", mod.id(), a.library(), t.toString());
                return null;
            }
        }
        return null;
    }

    private static List<Adapter> adapters() {
        if (adapters != null) return adapters;
        List<Adapter> on = new ArrayList<>();
        for (EntrypointContainer<Adapter> c : FabricLoader.getInstance().getEntrypointContainers("modhub:adapter", Adapter.class)) {
            try {
                Adapter a = c.getEntrypoint();
                Optional<ModContainer> lib = FabricLoader.getInstance().getModContainer(a.library());
                if (lib.isEmpty()) continue;
                var version = lib.get().getMetadata().getVersion();
                if (!VersionPredicate.parse(a.testedRange()).test(version)) {
                    LOG.info("Mod Hub: {} {} is outside the versions Mod Hub has checked ({}); its mods open their own screens",
                            a.library(), version.getFriendlyString(), a.testedRange());
                    continue;
                }
                on.add(a);
            } catch (Throwable t) {
                LOG.warn("Mod Hub: an adapter failed to load: {}", t.toString());
            }
        }
        return adapters = List.copyOf(on);
    }
}

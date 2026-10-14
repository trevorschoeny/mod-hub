package com.trevlar.modhub.prodcheck;

import com.trevlar.modhub.catalog.HubEntry;
import com.trevlar.modhub.catalog.ModMenuBridge;
import com.trevlar.modhub.catalog.ModCatalog;
import com.trevlar.modhub.catalog.ModInfo;
import com.trevlar.modhub.screen.ConfigScreen;
import com.trevlar.modhub.screen.ModListScreen;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A production-shaped check of the gear, never shipped: boots with a real mod set (Loom's
 * {@code prodCheck*} tasks), then for every mod that has a {@code modmenu} entrypoint presses the
 * gear the way the mod list does, logs which screen opened and screenshots it. Run once with
 * Mod Menu and once without, and the two logs should name the same screens.
 */
public final class ConfigGearCheck implements FabricClientGameTest {

    private static final Logger LOG = LoggerFactory.getLogger("modhub-prodcheck");

    @Override
    public void runTest(ClientGameTestContext ctx) {
        FabricLoader loader = FabricLoader.getInstance();
        String mode = loader.isModLoaded("modmenu") ? "with-modmenu" : "without-modmenu";
        LOG.info("MHCHECK mode={} standIn={}", mode, loader.isModLoaded("modhub_modmenu_api"));
        java.util.List<String> ids = new java.util.ArrayList<>(loader.getEntrypointContainers("modmenu", Object.class)
                .stream().map(c -> c.getProvider().getMetadata().getId()).distinct().sorted().toList());
        ids.add("minecraft");
        for (String id : ids) {
            // The mod's own entry, whether it's top-level, in a suite or in Auxiliary.
            HubEntry entry = ModCatalog.find("mod:" + id);
            ModInfo mod = entry == null ? null : entry.mods().getFirst();
            boolean gear = mod != null && mod.configurable();
            if (!gear) {
                LOG.info("MHCHECK {} {} gear=no (bridge says {})", mode, id, ModMenuBridge.hasScreen(id));
                continue;
            }
            ctx.setScreen(() -> ModListScreen.create(null));
            ctx.runOnClient(mc -> ConfigScreen.open(entry, mc.gui.screen()));
            ctx.waitTicks(10);
            String opened = ctx.computeOnClient(mc -> {
                Screen s = mc.gui.screen();
                return s == null ? "none" : s.getClass().getName();
            });
            LOG.info("MHCHECK {} {} gear=yes opened={}", mode, id, opened);
            ctx.takeScreenshot(mode + "-" + id);
        }
        ctx.setScreen(() -> null);
        LOG.info("MHCHECK {} done", mode);
    }
}

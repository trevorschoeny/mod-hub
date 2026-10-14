package com.trevlar.modhub;

import com.mojang.blaze3d.platform.InputConstants;
import com.trevlar.modhub.catalog.HubEntry;
import com.trevlar.modhub.catalog.HubState;
import com.trevlar.modhub.catalog.ModCatalog;
import com.trevlar.modhub.screen.ConfigScreen;
import com.trevlar.modhub.screen.ShortcutRow;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mod Hub's client half: declares the shortcut rows and watches the entries' keys. The Mods button
 * in the pause menu is {@code PauseScreenMixin}'s.
 */
public final class ModHubClient implements ClientModInitializer {

    // Keys held down last tick, so a key opens its entry once per press, not every tick.
    private static final Set<Integer> HELD = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ShortcutRow.register();
        ClientTickEvents.END_CLIENT_TICK.register(ModHubClient::keys);
    }

    // An entry's key opens its settings in game with no screen open, so never while typing in chat
    // or in another menu, since those are screens. Container screens would need
    // ScreenKeyboardEvents, if a key should also work over the inventory.
    private static void keys(Minecraft mc) {
        boolean inGame = mc.gui.screen() == null && mc.player != null;
        for (Map.Entry<String, Integer> k : HubState.keys().entrySet()) {
            int code = k.getValue();
            boolean down = inGame && InputConstants.isKeyDown(code);
            if (down && HELD.add(code)) {
                HubEntry e = ModCatalog.find(k.getKey());
                if (e != null && e.configurable()) ConfigScreen.open(e, null);
            } else if (!down) {
                HELD.remove(code);
            }
        }
    }
}

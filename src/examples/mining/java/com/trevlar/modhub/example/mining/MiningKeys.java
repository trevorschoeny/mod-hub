package com.trevlar.modhub.example.mining;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** A pretend key for the example mod, so Mod Hub's Keybindery button has something to show. Dev runtime only. */
public final class MiningKeys implements ClientModInitializer {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("modhub_example_mining", "controls"));

    public static final KeyMapping VEIN = new KeyMapping("key.modhub_example_mining.vein",
            InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY);

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(VEIN);
    }
}

package com.trevlar.modhub.example.lumber;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** A pretend key for the example mod, so Mod Hub's Keybindery button has something to show. Dev runtime only. */
public final class LumberKeys implements ClientModInitializer {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("modhub_example_lumber", "controls"));

    public static final KeyMapping FELL = new KeyMapping("key.modhub_example_lumber.fell",
            InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY);

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(FELL);
    }
}

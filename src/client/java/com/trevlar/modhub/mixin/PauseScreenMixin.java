package com.trevlar.modhub.mixin;

import com.trevlar.modhub.screen.ModListScreen;

import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Adds the Mods button to the pause menu's own row of small icon buttons (Report Bugs, Feedback,
 * Friends, Player Reporting), at its end. Vanilla builds that row as a {@code LinearLayout} and
 * hands it to the grid in its second three-argument {@code addChild}; this adds Mod Hub's button
 * to the row just before, so vanilla lays it out and centres it with the rest. MenuKit leaves the
 * pause menu's own layout alone, so this mixin is Mod Hub's.
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {

    private static final Identifier MODS_SPRITE = Identifier.fromNamespaceAndPath("modhub", "pause_menu/mods");

    @ModifyArg(method = "createPauseMenu",
            at = @At(value = "INVOKE", ordinal = 1,
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;ILnet/minecraft/client/gui/layouts/LayoutSettings;)Lnet/minecraft/client/gui/layouts/LayoutElement;"),
            index = 0)
    private LayoutElement modhub$addModsButton(LayoutElement element) {
        // The guard keeps this harmless if a future version adds another row at this call.
        if (element instanceof LinearLayout row) {
            Screen self = (Screen) (Object) this;
            row.addChild(SpriteIconButton.builder(Component.literal("Mod Hub"), b -> ModListScreen.open(self), true)
                    .width(20)
                    .sprite(MODS_SPRITE, 15, 15)
                    .withTootip()
                    .build());
        }
        return element;
    }
}

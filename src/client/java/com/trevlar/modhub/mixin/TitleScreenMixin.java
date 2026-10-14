package com.trevlar.modhub.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.trevlar.modhub.screen.ModListScreen;

import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the Mod Hub button to the title screen's row of small icon buttons (Friends, Language,
 * Accessibility), at its end: the same button, in the same kind of row, as in the pause menu.
 *
 * <p>Vanilla centres that row with {@code getHorizontalPosition(index, count, width)}, counting its
 * buttons in the local {@code numberOfButtons} but passing the count to each call as a constant.
 * This raises the local by one, passes the local instead of the constant, and adds the button at the
 * next index. Mod Menu's icon-style button joins the row through the same two locals, so with both
 * installed the row holds five and nothing overlaps.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    private TitleScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    protected abstract int getHorizontalPosition(int index, int count, int width);

    /** One more slot in the icon row. */
    @ModifyVariable(method = "init", at = @At("STORE"), name = "numberOfButtons")
    private int modhub$oneMoreIcon(int count) {
        return count + 1;
    }

    /** Lays vanilla's own icons out against the real count, not the constant 3. */
    @WrapOperation(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/TitleScreen;getHorizontalPosition(III)I"))
    private int modhub$countAllIcons(TitleScreen self, int index, int count, int width, Operation<Integer> original,
                                     @Local(name = "numberOfButtons") int numberOfButtons) {
        return original.call(self, index, numberOfButtons, width);
    }

    /** The button itself, after Accessibility, on the same line. */
    @Inject(method = "init", at = @At(value = "CONSTANT", args = "stringValue=menu.options"))
    private void modhub$addModsButton(CallbackInfo ci, @Local(name = "numberOfButtons") int numberOfButtons,
                                      @Local(name = "currentButton") LocalIntRef currentButton,
                                      @Local(name = "topPos") int topPos) {
        currentButton.set(currentButton.get() + 1);
        Screen self = this;
        SpriteIconButton button = SpriteIconButton.builder(Component.literal("Mod Hub"), b -> ModListScreen.open(self), true)
                .width(20)
                .sprite(ModListScreen.MODS_SPRITE, 15, 15)
                .withTootip()
                .build();
        button.setPosition(getHorizontalPosition(currentButton.get(), numberOfButtons, 20), topPos);
        addRenderableWidget(button);
    }
}

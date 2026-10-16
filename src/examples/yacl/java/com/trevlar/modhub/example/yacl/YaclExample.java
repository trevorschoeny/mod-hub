package com.trevlar.modhub.example.yacl;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionFlag;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumDropdownControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.ItemControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.BooleanController;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.awt.Color;

/**
 * A pretend mod whose settings screen is built with YACL, one of each option the YACL adapter draws,
 * in two categories with a collapsible group, so Mod Hub's YACL page can be checked in one place.
 * Values live in memory only: each change logs, standing in for a real mod's save. Dev runtime only.
 */
public final class YaclExample implements ModMenuApi {

    /**
     * A controller of the mod's own, standing in for one Mod Hub has never seen: it draws YACL's
     * on/off widget, but its class isn't one the adapter knows, so the adapter hosts it.
     */
    record CustomToggle(Option<Boolean> option) implements Controller<Boolean> {
        @Override public Component formatValue() { return Component.literal(option.pendingValue() ? "Yes" : "No"); }

        @Override
        public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> dim) {
            return new BooleanController(option, true).provideWidget(screen, dim);
        }
    }

    public enum Mode { OFF, GENTLE, FAST }

    static boolean particles = true, sounds = true;
    static int range = 16, stack = 32;
    static double volume = 0.5;
    static String greeting = "Hello";
    static Mode mode = Mode.GENTLE, fallback = Mode.OFF;
    static int saves;
    static Color glow = new Color(0x3FA7FF), tint = new Color(0x8044CC22, true);
    static Item favourite = Items.DIAMOND;
    static boolean custom = true;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Example YACL Mod"))
                .category(ConfigCategory.createBuilder().name(Component.literal("General"))
                        .option(LabelOption.create(Component.literal("Every change saves at once.")))
                        .option(Option.<Boolean>createBuilder().name(Component.literal("Particles"))
                                .description(OptionDescription.of(Component.literal("A YACL tick box.")))
                                .binding(true, () -> particles, v -> particles = v)
                                .controller(TickBoxControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder().name(Component.literal("Sounds"))
                                .description(OptionDescription.of(Component.literal("A YACL on/off button. Needs a restart.")))
                                .binding(true, () -> sounds, v -> sounds = v)
                                .flag(OptionFlag.GAME_RESTART)
                                .controller(BooleanControllerBuilder::create).build())
                        .option(Option.<Mode>createBuilder().name(Component.literal("Mode"))
                                .binding(Mode.GENTLE, () -> mode, v -> mode = v)
                                .controller(o -> EnumControllerBuilder.create(o).enumClass(Mode.class)).build())
                        .option(Option.<Mode>createBuilder().name(Component.literal("Fallback mode"))
                                .binding(Mode.OFF, () -> fallback, v -> fallback = v)
                                .controller(EnumDropdownControllerBuilder::create).build())
                        .option(Option.<Color>createBuilder().name(Component.literal("Glow colour"))
                                .description(OptionDescription.of(Component.literal("A YACL colour, no alpha.")))
                                .binding(new Color(0x3FA7FF), () -> glow, v -> glow = v)
                                .controller(ColorControllerBuilder::create).build())
                        .option(Option.<Color>createBuilder().name(Component.literal("Tint"))
                                .description(OptionDescription.of(Component.literal("A YACL colour with alpha.")))
                                .binding(new Color(0x8044CC22, true), () -> tint, v -> tint = v)
                                .controller(o -> ColorControllerBuilder.create(o).allowAlpha(true)).build())
                        .option(Option.<Item>createBuilder().name(Component.literal("Favourite item"))
                                .description(OptionDescription.of(Component.literal("The item you like best.")))
                                .binding(Items.DIAMOND, () -> favourite, v -> favourite = v)
                                .controller(ItemControllerBuilder::create).build())
                        .option(Option.<Boolean>createBuilder().name(Component.literal("Custom widget"))
                                .description(OptionDescription.of(Component.literal("A controller of this mod's own, hosted in its row.")))
                                .binding(true, () -> custom, v -> custom = v)
                                .customController(CustomToggle::new).build())
                        .option(ButtonOption.createBuilder().name(Component.literal("Say hello"))
                                .action((screen, button) -> System.out.println("[example yacl] hello")).build())
                        .build())
                .category(ConfigCategory.createBuilder().name(Component.literal("Numbers"))
                        .option(Option.<Integer>createBuilder().name(Component.literal("Range"))
                                .binding(16, () -> range, v -> range = v)
                                .controller(o -> IntegerSliderControllerBuilder.create(o).range(4, 64).step(4)
                                        .formatValue(v -> Component.literal(v + " blocks"))).build())
                        .option(Option.<Double>createBuilder().name(Component.literal("Volume"))
                                .binding(0.5, () -> volume, v -> volume = v)
                                .controller(o -> DoubleSliderControllerBuilder.create(o).range(0.0, 1.0).step(0.05)
                                        .formatValue(v -> Component.literal(Math.round(v * 100) + "%"))).build())
                        .group(OptionGroup.createBuilder().name(Component.literal("More"))
                                .description(OptionDescription.of(Component.literal("A collapsible YACL group.")))
                                .collapsed(true)
                                .option(Option.<Integer>createBuilder().name(Component.literal("Stack size"))
                                        .binding(32, () -> stack, v -> stack = v)
                                        .controller(o -> IntegerFieldControllerBuilder.create(o).range(1, 64)).build())
                                .option(Option.<String>createBuilder().name(Component.literal("Greeting"))
                                        .binding("Hello", () -> greeting, v -> greeting = v)
                                        .controller(StringControllerBuilder::create).build())
                                .build())
                        .build())
                .save(() -> System.out.println("[example yacl] saved " + (++saves) + ": particles=" + particles
                        + " sounds=" + sounds + " mode=" + mode + " fallback=" + fallback + " range=" + range
                        + " volume=" + volume + " stack=" + stack + " greeting=" + greeting + " glow=" + Integer.toHexString(glow.getRGB())
                + " tint=" + Integer.toHexString(tint.getRGB()) + " favourite=" + favourite + " custom=" + custom))
                .build()
                .generateScreen(parent);
    }
}

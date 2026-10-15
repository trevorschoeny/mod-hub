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

import net.minecraft.network.chat.Component;

/**
 * A pretend mod whose settings screen is built with YACL, one of each option the YACL adapter draws,
 * in two categories with a collapsible group, so Mod Hub's YACL page can be checked in one place.
 * Values live in memory only: each change logs, standing in for a real mod's save. Dev runtime only.
 */
public final class YaclExample implements ModMenuApi {

    public enum Mode { OFF, GENTLE, FAST }

    static boolean particles = true, sounds = true;
    static int range = 16, stack = 32;
    static double volume = 0.5;
    static String greeting = "Hello";
    static Mode mode = Mode.GENTLE, fallback = Mode.OFF;
    static int saves;

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
                        .option(ButtonOption.createBuilder().name(Component.literal("Say hello"))
                                .action((screen, button) -> System.out.println("[example yacl] hello")).build())
                        .build())
                .category(ConfigCategory.createBuilder().name(Component.literal("Numbers"))
                        .option(Option.<Integer>createBuilder().name(Component.literal("Range"))
                                .binding(16, () -> range, v -> range = v)
                                .controller(o -> IntegerSliderControllerBuilder.create(o).range(4, 64).step(4)).build())
                        .option(Option.<Double>createBuilder().name(Component.literal("Volume"))
                                .binding(0.5, () -> volume, v -> volume = v)
                                .controller(o -> DoubleSliderControllerBuilder.create(o).range(0.0, 1.0).step(0.05)).build())
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
                        + " volume=" + volume + " stack=" + stack + " greeting=" + greeting))
                .build()
                .generateScreen(parent);
    }
}

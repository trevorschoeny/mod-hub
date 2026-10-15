package com.trevlar.modhub.example.cloth;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A pretend mod whose settings screen is built with Cloth Config, one of each entry the Cloth
 * adapter draws, in two categories with a sub-category. Values live in memory only: the saving
 * action logs, standing in for a real mod's save. Dev runtime only.
 */
public final class ClothExample implements ModMenuApi {

    public enum Mode { OFF, GENTLE, FAST }

    static boolean particles = true;
    static int range = 16, stack = 32;
    static long seed = 42;
    static double volume = 0.5;
    static String greeting = "Hello";
    static Mode mode = Mode.GENTLE;
    static int saves;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                    .setTitle(Component.literal("Example Cloth Mod"))
                    .setSavingRunnable(() -> System.out.println("[example cloth] saved " + (++saves) + ": particles="
                            + particles + " mode=" + mode + " range=" + range + " stack=" + stack + " seed=" + seed
                            + " volume=" + volume + " greeting=" + greeting));
            ConfigEntryBuilder e = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
            general.addEntry(e.startTextDescription(Component.literal("Every change saves at once.")).build());
            general.addEntry(e.startBooleanToggle(Component.literal("Particles"), particles).setDefaultValue(true)
                    .setTooltip(Component.literal("A Cloth toggle.")).setSaveConsumer(v -> particles = v).build());
            general.addEntry(e.startEnumSelector(Component.literal("Mode"), Mode.class, mode).setDefaultValue(Mode.GENTLE)
                    .setSaveConsumer(v -> mode = v).build());
            general.addEntry(e.startStrField(Component.literal("Greeting"), greeting).setDefaultValue("Hello")
                    .setSaveConsumer(v -> greeting = v).build());
            ConfigCategory numbers = builder.getOrCreateCategory(Component.literal("Numbers"));
            numbers.addEntry(e.startIntSlider(Component.literal("Range"), range, 4, 64).setDefaultValue(16)
                    .setSaveConsumer(v -> range = v).build());
            numbers.addEntry(e.startDoubleField(Component.literal("Volume"), volume).setDefaultValue(0.5).setMin(0.0).setMax(1.0)
                    .setSaveConsumer(v -> volume = v).build());
            numbers.addEntry(e.startSubCategory(Component.literal("More"), List.of(
                    e.startIntField(Component.literal("Stack size"), stack).setDefaultValue(32).setMin(1).setMax(64)
                            .setSaveConsumer(v -> stack = v).build(),
                    e.startLongField(Component.literal("Seed"), seed).setDefaultValue(42L).setRequirement(null)
                            .requireRestart().setSaveConsumer(v -> seed = v).build()
            )).setExpanded(false).build());
            return builder.build();
        };
    }
}

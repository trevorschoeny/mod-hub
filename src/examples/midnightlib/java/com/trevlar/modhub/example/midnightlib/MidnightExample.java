package com.trevlar.modhub.example.midnightlib;

import eu.midnightdust.lib.config.MidnightConfig;

import net.fabricmc.api.ClientModInitializer;

/**
 * A pretend mod whose settings are MidnightLib fields, one of each kind the MidnightLib adapter
 * draws, in two categories, one shown only while another is on. MidnightLib registers its screen
 * with Mod Menu by itself. Its file is config/modhub_example_midnightlib.json. Dev runtime only.
 */
public final class MidnightExample extends MidnightConfig implements ClientModInitializer {

    public enum Mode { OFF, GENTLE, FAST }

    @Comment(category = "general") public static Comment intro;
    @Entry(category = "general") public static boolean particles = true;
    @Entry(category = "general") public static Mode mode = Mode.GENTLE;
    @Condition(requiredOption = "particles", visibleButLocked = true)
    @Entry(category = "general") public static boolean sparkles = false;
    @Entry(category = "general") public static String greeting = "Hello";
    @Entry(category = "numbers", min = 4, max = 64, isSlider = true) public static int range = 16;
    @Entry(category = "numbers", min = 0, max = 1, isSlider = true) public static double volume = 0.5;
    @Entry(category = "numbers", min = 1, max = 64) public static int stack = 32;

    @Override
    public void onInitializeClient() {
        MidnightConfig.init("modhub_example_midnightlib", MidnightExample.class);
    }
}

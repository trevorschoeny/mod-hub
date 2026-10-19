package com.trevlar.modhub.example.midnightlib;

import eu.midnightdust.lib.config.MidnightConfig;

import net.fabricmc.api.ClientModInitializer;

import java.util.ArrayList;
import java.util.List;

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
    @Entry(category = "general", isColor = true, width = 7, min = 7) public static String glow = "#3fa7ff";
    @Entry(category = "numbers", min = 4, max = 64, isSlider = true) public static int range = 16;
    @Entry(category = "numbers", min = 0, max = 1, isSlider = true) public static double volume = 0.5;
    @Entry(category = "numbers", min = 1, max = 64) public static int stack = 32;
    @Entry(category = "general") public static List<String> greetings = new ArrayList<>(List.of("Hello", "Hi"));
    @Entry(category = "numbers", min = 1, max = 99) public static List<Integer> lucky = new ArrayList<>(List.of(7, 13));

    @Override
    public void onInitializeClient() {
        MidnightConfig.init("modhub_example_midnightlib", MidnightExample.class);
    }
}

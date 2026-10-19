package com.trevlar.modhub.example.fcap;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;

import net.fabricmc.api.ClientModInitializer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * A pretend mod whose settings are a NeoForge config spec through Forge Config API Port, one of
 * each value the adapter draws, with a section. Its file is config/modhub_example_fcap-client.toml.
 * Dev runtime only.
 */
public final class FcapExample implements ClientModInitializer {

    public enum Mode { OFF, GENTLE, FAST }

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    static final ModConfigSpec.BooleanValue PARTICLES = B.comment("Show particles.").define("particles", true);
    static final ModConfigSpec.EnumValue<Mode> MODE = B.comment("How fast things go.").defineEnum("mode", Mode.GENTLE);
    static final ModConfigSpec.ConfigValue<String> GREETING = B.comment("What to say.").define("greeting", "Hello");
    static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> GREETINGS = B.comment("Ways to say hello.")
            .defineListAllowEmpty("greetings", java.util.List.of("Hello", "Hi"), () -> "", o -> o instanceof String);
    static final ModConfigSpec.IntValue RANGE;
    static final ModConfigSpec.DoubleValue VOLUME;
    static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> LUCKY;
    static {
        B.comment("Numbers, in a section of their own.").push("numbers");
        RANGE = B.comment("How far, in blocks.").defineInRange("range", 16, 4, 64);
        VOLUME = B.comment("How loud.").gameRestart().defineInRange("volume", 0.5, 0.0, 1.0);
        LUCKY = B.comment("Lucky numbers, 1 to 99.").defineList("lucky", java.util.List.of(7, 13), () -> 1,
                o -> o instanceof Integer i && i >= 1 && i <= 99);
        B.pop();
    }
    static final ModConfigSpec SPEC = B.build();

    @Override
    public void onInitializeClient() {
        ConfigRegistry.INSTANCE.register("modhub_example_fcap", ModConfig.Type.CLIENT, SPEC);
        ConfigScreenFactoryRegistry.INSTANCE.register("modhub_example_fcap", ConfigurationScreen::new);
    }
}

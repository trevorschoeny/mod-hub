package com.trevlar.modhub;

import com.trevlar.modhub.api.Settings;

import net.fabricmc.api.ModInitializer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mod Hub's common half: where every mod's settings are registered, on both sides.
 *
 * <p>Nothing here touches a screen, so a dedicated server loads it and reads server settings from
 * its own files. Sending them to clients is not built yet.
 */
public final class ModHub implements ModInitializer {

    public static final String MOD_ID = "modhub";

    private static final Map<String, Settings> SETTINGS = new ConcurrentHashMap<>();

    /**
     * The settings for {@code modId}, made on first call. Hold the result in a static field and
     * declare settings on it:
     * {@code public static final Settings S = ModHub.settings("hivesight");}
     */
    public static Settings settings(String modId) {
        return SETTINGS.computeIfAbsent(modId, Settings::new);
    }

    /** Every mod that has declared settings. */
    public static Collection<Settings> allSettings() {
        return Collections.unmodifiableCollection(SETTINGS.values());
    }

    @Override
    public void onInitialize() {}
}

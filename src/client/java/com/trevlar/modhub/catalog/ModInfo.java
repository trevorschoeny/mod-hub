package com.trevlar.modhub.catalog;

import com.trevlar.modhub.api.Settings;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * What the mod list knows about one installed mod, read once from its {@code fabric.mod.json}.
 *
 * @param settings     its Mod Hub settings, or null when it isn't on Mod Hub
 * @param modMenuScreen whether Mod Menu can open a screen of its own for it (only ever true when
 *                     Mod Menu is installed: the screen comes through Mod Menu's own code)
 * @param suiteId      its {@code custom.modhub.suite}, or null
 * @param links        label to URL, in the order shown (Homepage, Source, Issues, then Mod Menu's)
 */
public record ModInfo(String id, String name, String version, String description, List<String> authors,
                      Map<String, String> links, @Nullable Settings settings, boolean modMenuScreen,
                      @Nullable String suiteId) {

    /** Whether it has settings Mod Hub can open, natively or through Mod Menu. */
    public boolean configurable() {
        return settings != null || modMenuScreen;
    }
}

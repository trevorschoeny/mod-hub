package com.trevlar.modhub.compat.fcap;

import com.trevlar.modhub.page.Adapter;
import com.trevlar.modhub.page.ConfigPage;

import net.minecraft.client.gui.screens.Screen;

import org.jetbrains.annotations.Nullable;

/**
 * Forge Config API Port's config screens (NeoForge's {@code ModConfigSpec} on Fabric) as Mod Hub
 * pages. Mentions none of its types, so it loads safely without it; {@link SpecPages} does the reading.
 */
public final class ForgeConfigApiPortAdapter implements Adapter {

    @Override public String library() { return "forgeconfigapiport"; }

    /** Built against 26.3.1. Widen after checking a new version against the Forge Config API Port example mod. */
    @Override public String testedRange() { return ">=26.3.0 <26.4"; }

    @Override
    public @Nullable ConfigPage translate(String modId, Screen screen) {
        return SpecPages.translate(modId, screen);
    }
}

package com.trevlar.modhub.compat.cloth;

import com.trevlar.modhub.page.Adapter;
import com.trevlar.modhub.page.ConfigPage;

import net.minecraft.client.gui.screens.Screen;

import org.jetbrains.annotations.Nullable;

/**
 * Cloth Config's screens (and AutoConfig's, which are built the same way) as Mod Hub pages.
 * Mentions none of Cloth's types, so it loads safely without Cloth; {@link ClothPages} does the
 * reading.
 */
public final class ClothConfigAdapter implements Adapter {

    @Override public String library() { return "cloth-config"; }

    /** Built against 26.3.159. Widen after checking a new version against the Cloth Config example mod. */
    @Override public String testedRange() { return ">=26.3.0 <26.4"; }

    @Override
    public @Nullable ConfigPage translate(String modId, Screen screen) {
        return ClothPages.translate(screen);
    }
}

package com.trevlar.modhub.compat.midnightlib;

import com.trevlar.modhub.page.Adapter;
import com.trevlar.modhub.page.ConfigPage;

import net.minecraft.client.gui.screens.Screen;

import org.jetbrains.annotations.Nullable;

/**
 * MidnightLib's screens as Mod Hub pages. Mentions none of MidnightLib's types, so it loads safely
 * without it; {@link MidnightPages} does the reading.
 */
public final class MidnightLibAdapter implements Adapter {

    @Override public String library() { return "midnightlib"; }

    /** Built against 1.10.0+26.3. Widen after checking a new version against the MidnightLib example mod. */
    @Override public String testedRange() { return ">=1.10.0 <1.11"; }

    @Override
    public @Nullable ConfigPage translate(String modId, Screen screen) {
        return MidnightPages.translate(screen);
    }
}

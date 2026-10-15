package com.trevlar.modhub.compat.owo;

import com.trevlar.modhub.page.Adapter;
import com.trevlar.modhub.page.ConfigPage;

import net.minecraft.client.gui.screens.Screen;

import org.jetbrains.annotations.Nullable;

/**
 * owo-config's screens as Mod Hub pages. Mentions none of owo's types, so it loads safely without
 * owo; {@link OwoPages} does the reading.
 */
public final class OwoConfigAdapter implements Adapter {

    @Override public String library() { return "owo"; }

    /**
     * Off: matches no version. Written against owo-lib 0.13.1+26.2, the newest build when this was
     * made, since owo had no 26.3 build yet (2026-10-14). When one ships: pin it in build.gradle's
     * compat table, add the owo example mod and scene, check them, and set this to the range checked.
     */
    @Override public String testedRange() { return "<0.0.0"; }

    @Override
    public @Nullable ConfigPage translate(String modId, Screen screen) {
        return OwoPages.translate(screen);
    }
}

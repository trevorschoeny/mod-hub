package com.trevlar.modhub.compat.yacl;

import com.trevlar.modhub.page.Adapter;
import com.trevlar.modhub.page.ConfigPage;

import net.minecraft.client.gui.screens.Screen;

import org.jetbrains.annotations.Nullable;

/**
 * YACL 3's screens as Mod Hub pages. Mentions none of YACL's types, so it loads safely without
 * YACL; {@link YaclPages} does the reading.
 */
public final class YaclAdapter implements Adapter {

    @Override public String library() { return "yet_another_config_lib_v3"; }

    /** Built against 3.9.7+26.3. Widen after checking a new version against the YACL example mod. */
    @Override public String testedRange() { return ">=3.9.6 <3.10"; }

    @Override
    public @Nullable ConfigPage translate(Screen screen) {
        return YaclPages.translate(screen);
    }
}

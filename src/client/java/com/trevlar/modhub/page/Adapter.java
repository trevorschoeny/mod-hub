package com.trevlar.modhub.page;

import net.minecraft.client.gui.screens.Screen;

import org.jetbrains.annotations.Nullable;

/**
 * Translates one config library's screen into a {@link ConfigPage}, so a mod built on that library
 * opens as a Mod Hub page. One per library, each in its own source set ({@code src/compat/<library>})
 * compiled against a pinned version of it, and listed under Mod Hub's {@code modhub:adapter}
 * entrypoint.
 *
 * <p><b>This class is loaded whether or not the library is installed</b>, so an implementation must
 * not mention the library's types itself: it hands the screen to a second class that does, which
 * the JVM only loads once {@link Pages} has checked the library is there and in range.
 */
public interface Adapter {

    /** The library's mod id, as the loader knows it. */
    String library();

    /**
     * The library versions this adapter was built and checked against, as a Fabric version
     * predicate (e.g. {@code ">=3.9.6 <3.10"}). Outside it, the adapter stays off and the library's
     * mods open their own screens.
     */
    String testedRange();

    /**
     * {@code screen} as a page, or null when it isn't this library's screen. An exception, or a
     * page with something on it Mod Hub can't draw, also sends the mod to its own screen.
     */
    @Nullable ConfigPage translate(Screen screen);
}

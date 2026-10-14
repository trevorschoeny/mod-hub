package com.trevlar.modhub.catalog;

import java.util.List;

/**
 * One entry in the mod list's sidebar: a mod on its own, a developer's suite, or Auxiliary. Its
 * {@link #id} is how Mod Hub's own state (shortcuts, keys) refers to
 * it, so it never changes while the entry exists.
 */
public sealed interface HubEntry {

    /** {@code mod:<modid>}, {@code suite:<suiteid>}, or {@code auxiliary}. */
    String id();

    String name();

    /** The mods it opens, in order: one for a mod, the members for the rest. */
    List<ModInfo> mods();

    /** Whether Configure has anything to open: true when any of its mods has settings. */
    default boolean configurable() {
        return mods().stream().anyMatch(ModInfo::configurable);
    }

    /**
     * The key of the icon that stands for it, for {@code ModIcons.of}: a mod's own id, or a
     * suite's. Null for Auxiliary, which has none.
     */
    @org.jetbrains.annotations.Nullable String iconKey();

    record Single(ModInfo mod) implements HubEntry {
        @Override public String id() { return "mod:" + mod.id(); }
        @Override public String name() { return mod.name(); }
        @Override public String iconKey() { return mod.id(); }
        @Override public List<ModInfo> mods() { return List.of(mod); }
    }

    /**
     * A developer's suite. {@code iconKey} is {@code suite:<id>} when a member declares a
     * {@code suiteIcon}, or else its first member's id, so a suite with no icon of its own wears
     * its first mod's.
     */
    record Suite(String suiteId, String name, List<ModInfo> mods, String iconKey) implements HubEntry {
        @Override public String id() { return "suite:" + suiteId; }
    }


    /** Every mod with no settings, together. Never configurable, never a shortcut. */
    record Auxiliary(List<ModInfo> mods) implements HubEntry {
        public static final String ID = "auxiliary";
        @Override public String id() { return ID; }
        @Override public String name() { return "Auxiliary"; }
        @Override public String iconKey() { return null; }
        @Override public boolean configurable() { return false; }
    }
}

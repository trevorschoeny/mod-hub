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

    /** The mod whose icon stands for it: itself, or its first member. Null when it has no mods. */
    default ModInfo iconMod() {
        return mods().isEmpty() ? null : mods().getFirst();
    }

    record Single(ModInfo mod) implements HubEntry {
        @Override public String id() { return "mod:" + mod.id(); }
        @Override public String name() { return mod.name(); }
        @Override public List<ModInfo> mods() { return List.of(mod); }
    }

    record Suite(String suiteId, String name, List<ModInfo> mods) implements HubEntry {
        @Override public String id() { return "suite:" + suiteId; }
    }


    /** Every mod with no settings, together. Never configurable, never a shortcut. */
    record Auxiliary(List<ModInfo> mods) implements HubEntry {
        public static final String ID = "auxiliary";
        @Override public String id() { return ID; }
        @Override public String name() { return "Auxiliary"; }
        @Override public boolean configurable() { return false; }
    }
}

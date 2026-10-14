package com.terraformersmc.modmenu.api;

/** Stand-in for Mod Menu's enum, so an update checker class links. Mod Hub has no channel setting. */
public enum UpdateChannel {
    ALPHA,
    BETA,
    RELEASE;

    public static UpdateChannel getUserPreference() {
        return RELEASE;
    }
}

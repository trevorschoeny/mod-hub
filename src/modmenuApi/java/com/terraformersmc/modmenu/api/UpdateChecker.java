package com.terraformersmc.modmenu.api;

/** Stand-in for Mod Menu's type, so an entrypoint that overrides {@code getUpdateChecker} links. Never called. */
public interface UpdateChecker {
    UpdateInfo checkForUpdates();
}

package com.terraformersmc.modmenu.api;

import net.minecraft.network.chat.Component;

/** Stand-in for Mod Menu's type, so an update checker class links. Never called. */
public interface UpdateInfo {
    boolean isUpdateAvailable();

    default Component getUpdateMessage() {
        return null;
    }

    String getDownloadLink();

    UpdateChannel getUpdateChannel();
}

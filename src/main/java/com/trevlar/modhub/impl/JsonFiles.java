package com.trevlar.modhub.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Mod Hub's one way to write a file: to a temp file beside it, then moved into place, so a crash
 * mid-write leaves the old file whole instead of half a new one. Every file Mod Hub writes (each
 * mod's settings, Mod Hub's own list state) goes through here.
 */
public final class JsonFiles {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonFiles() {}

    public static void write(Path file, JsonElement json) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(json), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

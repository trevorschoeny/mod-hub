package com.trevlar.modhub.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trevlar.modhub.impl.JsonFiles;

import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One mod's settings and the one file they live in. Get it from {@link com.trevlar.modhub.ModHub#settings}
 * in common code, then declare each setting on it, in the order the menu should show them:
 *
 * <pre>{@code
 * public static final Settings S = ModHub.settings("offrail");
 * public static final Setting<Boolean> FREE_PLACEMENT = S.bool("railFreePlacement", true).server();
 * }</pre>
 *
 * <p><b>The file</b> is {@code config/<modid>/config.json}, plain JSON with a top-level
 * {@code "version"}: the layout the Trevlar mods already use, so a mod moving onto Mod Hub declares
 * its settings with the keys its file has today and the file loads unchanged.
 *
 * <p><b>Tolerant, both ways.</b>
 * <ul>
 *   <li>A missing file or key is the default.</li>
 *   <li>A key the mod doesn't declare (from a newer version, or another tool) is kept and written
 *       back, so a downgrade loses nothing.</li>
 *   <li>A value of the wrong type is the default, logged; the other settings still load.</li>
 *   <li>A file that isn't JSON at all is moved aside to {@code config.json.bak} and the defaults
 *       load, so nothing the player wrote is destroyed.</li>
 *   <li>Writes go to a temp file first and are moved into place, so a crash mid-write never
 *       leaves half a file.</li>
 * </ul>
 *
 * <p>The file is read the first time any of its settings is read, which is after the declaring
 * class has finished initialising, so every setting is declared by then.
 */
public final class Settings {

    static final Logger LOG = LoggerFactory.getLogger("modhub");

    private final String modId;
    private Path file;
    private final List<Setting<?>> settings = new ArrayList<>();

    // The file as last read, so keys this mod doesn't declare survive a save.
    private JsonObject stored = new JsonObject();
    private boolean loaded;

    /** Use {@link com.trevlar.modhub.ModHub#settings}. */
    public Settings(String modId) {
        this.modId = Objects.requireNonNull(modId, "modId");
        this.file = FabricLoader.getInstance().getConfigDir().resolve(modId).resolve("config.json");
    }

    /** For the self-check: settings kept at {@code file}, without Fabric Loader. */
    Settings(String modId, Path file) {
        this.modId = Objects.requireNonNull(modId, "modId");
        this.file = file;
    }

    public String modId() { return modId; }

    /** Every setting, in declaration order. */
    public List<Setting<?>> all() { return Collections.unmodifiableList(settings); }

    /**
     * Keeps a file at a path other than {@code config/<modid>/config.json}, relative to the
     * config folder: for a mod whose file already lives elsewhere ({@code "inventoryplus/config.json"}).
     * Call before declaring any setting.
     */
    public Settings file(String relativePath) {
        if (loaded) throw new IllegalStateException("Mod Hub: " + modId + ".file(...) after its file was read");
        this.file = FabricLoader.getInstance().getConfigDir().resolve(relativePath);
        return this;
    }

    // Declaring

    public Setting.Bool bool(String key, boolean defaultValue) {
        return add(new Setting.Bool(this, key, defaultValue));
    }

    public Setting.Int intRange(String key, int defaultValue, int min, int max) {
        return add(new Setting.Int(this, key, defaultValue, min, max));
    }

    public <E extends Enum<E>> Setting.Choice<E> choice(String key, E defaultValue) {
        return add(new Setting.Choice<>(this, key, defaultValue));
    }

    private synchronized <S extends Setting<?>> S add(S setting) {
        for (Setting<?> s : settings) {
            if (s.key().equals(setting.key())) {
                throw new IllegalArgumentException("Mod Hub: " + modId + " declares '" + setting.key() + "' twice");
            }
        }
        settings.add(setting);
        // Declared after the file was read (rare: a setting in a class loaded later): take its
        // value from what was read, the same as the rest.
        if (loaded) setting.load(stored.get(setting.key()));
        return setting;
    }

    /** Puts every setting back to its default. */
    public void resetAll() {
        for (Setting<?> s : settings) s.reset();
    }

    // The file

    synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        stored = readFile();
        for (Setting<?> s : settings) s.load(stored.get(s.key()));
    }

    private JsonObject readFile() {
        if (!Files.exists(file)) return new JsonObject();
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (root.isJsonObject()) return root.getAsJsonObject();
            throw new IllegalStateException("not a JSON object");
        } catch (Exception e) {
            // Unreadable: keep the player's file aside rather than overwrite it on the next save.
            Path bak = file.resolveSibling(file.getFileName() + ".bak");
            LOG.error("Mod Hub: {} can't be read ({}); moved to {} and using the defaults.", file, e.getMessage(), bak);
            try {
                Files.move(file, bak, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveFailed) {
                LOG.error("Mod Hub: couldn't move {} aside", file, moveFailed);
            }
            return new JsonObject();
        }
    }

    /**
     * Writes every setting, on top of what was read, through a temp file.
     *
     * <p>Writes on every change, as the Trevlar mods' own files do. A slider drag writes once per
     * step, and files are a few hundred bytes. Debounce if a mod ever has a big one.
     */
    synchronized void save() {
        JsonObject out = stored.deepCopy();
        if (!out.has("version")) out.addProperty("version", 1);
        for (Setting<?> s : settings) out.add(s.key(), s.toJson());
        stored = out;
        try {
            JsonFiles.write(file, out);
        } catch (IOException e) {
            LOG.error("Mod Hub: couldn't save {}", file, e);
        }
    }
}

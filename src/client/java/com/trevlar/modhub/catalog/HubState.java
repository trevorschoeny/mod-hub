package com.trevlar.modhub.catalog;

import com.google.gson.JsonParser;
import com.trevlar.modhub.impl.JsonFiles;

import net.fabricmc.loader.api.FabricLoader;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the player sets about the mod list itself, as opposed to inside a mod: which entries
 * have a shortcut and where, the shortcut order, and each entry's key. Kept in
 * {@code config/modhub/list.json}, apart from Mod Hub's own settings ({@code config.json}).
 *
 * <p>Entries are named by {@link HubEntry#id()}. A shortcut or key for a mod that isn't installed
 * any more is kept, not dropped, so it comes back if the mod does.
 *
 * <p>Every change saves at once, like every setting.
 */
public final class HubState {

    private static final Logger LOG = LoggerFactory.getLogger("modhub");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("modhub").resolve("list.json");

    // The file's shape. Gson fills these from the file; a missing field keeps its default.

    static final class Shortcut {
        boolean pause;
        boolean inventory;
    }

    private static final class Data {
        int version = 1;
        Map<String, Shortcut> shortcuts = new LinkedHashMap<>();
        /** Entry ids with a shortcut anywhere, left to right. */
        List<String> order = new ArrayList<>();
        /** Entry id to GLFW key code. */
        Map<String, Integer> keys = new LinkedHashMap<>();
    }

    private static Data data = load();

    private HubState() {}

    // Shortcuts

    public static boolean pause(String entry) {
        Shortcut s = data.shortcuts.get(entry);
        return s != null && s.pause;
    }

    public static boolean inventory(String entry) {
        Shortcut s = data.shortcuts.get(entry);
        return s != null && s.inventory;
    }

    public static void setPause(String entry, boolean on) {
        shortcut(entry).pause = on;
        tidyShortcut(entry);
    }

    public static void setInventory(String entry, boolean on) {
        shortcut(entry).inventory = on;
        tidyShortcut(entry);
    }

    /** Every entry with a shortcut anywhere, in the player's order. */
    public static List<String> shortcutOrder() {
        return List.copyOf(data.order);
    }

    /**
     * Moves {@code entry} one place left ({@code -1}) or right ({@code +1}) in the shortcut order.
     * Nothing calls it yet: how reordering should look is still open.
     */
    public static void move(String entry, int delta) {
        int i = data.order.indexOf(entry);
        int j = i + delta;
        if (i < 0 || j < 0 || j >= data.order.size()) return;
        data.order.set(i, data.order.get(j));
        data.order.set(j, entry);
        save();
    }

    private static Shortcut shortcut(String entry) {
        return data.shortcuts.computeIfAbsent(entry, e -> new Shortcut());
    }

    /** A new shortcut goes at the end; one that's off everywhere leaves the order. Then saves. */
    private static void tidyShortcut(String entry) {
        Shortcut s = data.shortcuts.get(entry);
        boolean anywhere = s != null && (s.pause || s.inventory);
        if (!anywhere) {
            data.shortcuts.remove(entry);
            data.order.remove(entry);
        } else if (!data.order.contains(entry)) {
            data.order.add(entry);
        }
        save();
    }

    // Keys

    /** The key that opens {@code entry}, or null. */
    public static @Nullable Integer key(String entry) {
        return data.keys.get(entry);
    }

    /** Binds {@code key} to {@code entry}, or unbinds it with null. */
    public static void setKey(String entry, @Nullable Integer key) {
        if (key == null) data.keys.remove(entry);
        else data.keys.put(entry, key);
        save();
    }

    public static Map<String, Integer> keys() {
        return Map.copyOf(data.keys);
    }

    // The file

    private static Data load() {
        if (!Files.exists(FILE)) return new Data();
        try {
            Data d = JsonFiles.GSON.fromJson(
                    JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)), Data.class);
            return d == null ? new Data() : d;
        } catch (Exception e) {
            Path bak = FILE.resolveSibling("list.json.bak");
            LOG.error("Mod Hub: {} can't be read ({}); moved to {} and starting fresh.", FILE, e.getMessage(), bak);
            try {
                Files.move(FILE, bak, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveFailed) {
                LOG.error("Mod Hub: couldn't move {} aside", FILE, moveFailed);
            }
            return new Data();
        }
    }

    private static void save() {
        try {
            JsonFiles.write(FILE, JsonFiles.GSON.toJsonTree(data));
        } catch (IOException e) {
            LOG.error("Mod Hub: couldn't save {}", FILE, e);
        }
    }
}

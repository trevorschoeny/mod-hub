package com.trevlar.modhub.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The settings file's promises (Settings javadoc, "Tolerant, both ways"), each once. */
class SettingsFileTest {

    enum Mode { AUTO, MANUAL }

    @TempDir Path dir;

    @Test
    void missingFileIsDefaults() {
        Settings s = new Settings("t", dir.resolve("config.json"));
        Setting<Boolean> b = s.bool("b", true);
        assertTrue(b.get());
        assertFalse(Files.exists(dir.resolve("config.json")), "reading alone writes nothing");
    }

    @Test
    void badValuesFallBackAndUnknownKeysSurvive() throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{\"version\":1,\"b\":\"yes\",\"n\":999,\"m\":\"manual\",\"fromNewer\":42}");
        Settings s = new Settings("t", file);
        Setting<Boolean> b = s.bool("b", true);
        Setting<Integer> n = s.intRange("n", 5, 1, 10);
        Setting<Mode> m = s.choice("m", Mode.AUTO);

        assertTrue(b.get(), "a string where a boolean goes is the default");
        assertEquals(10, n.get(), "out of range clamps");
        assertEquals(Mode.MANUAL, m.get(), "choices read case-insensitively");

        n.set(3);
        JsonObject saved = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals(42, saved.get("fromNewer").getAsInt(), "a key this mod doesn't declare is kept");
        assertEquals(3, saved.get("n").getAsInt());
        n.set(-5);
        assertEquals(1, n.get(), "set clamps too");
    }

    @Test
    void unreadableFileIsKeptAside() throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, "{ not json");
        Settings s = new Settings("t", file);
        Setting<Integer> n = s.intRange("n", 5, 1, 10);
        assertEquals(5, n.get());
        assertEquals("{ not json", Files.readString(dir.resolve("config.json.bak")));
        assertFalse(Files.exists(file));
    }
}

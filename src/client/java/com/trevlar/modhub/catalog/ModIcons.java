package com.trevlar.modhub.catalog;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Each mod's own icon (the {@code icon} in its {@code fabric.mod.json}), loaded once from its jar
 * into a texture the screens draw at any size. The same picture is the big icon in a mod's
 * details, the tiny one beside its name, and its shortcut button's face.
 *
 * <p>Not atlas sprites: mod icons are arbitrary PNGs, so each is its own texture, which is why
 * Mod Hub draws them with its own elements ({@code ModIconImage}, {@code ModIconButton}) rather
 * than MenuKit's {@code Icon}, which takes sprites.
 */
public final class ModIcons {

    private static final Logger LOG = LoggerFactory.getLogger("modhub");

    /** A loaded icon: its texture and its square size in pixels. */
    public record Icon(Identifier texture, int size) {}

    /** By mod id. A mod with no usable icon maps to empty, so it's only tried once. */
    private static final Map<String, Optional<Icon>> CACHE = new HashMap<>();

    private ModIcons() {}

    /** Suites' declared icons, by suite id: the member mod whose jar has it, and its path there. */
    private static final Map<String, String[]> SUITES = new HashMap<>();

    /** Where suite {@code suiteId}'s {@code suiteIcon} is: in {@code ownerModId}'s jar, at {@code path}. */
    static void declareSuite(String suiteId, String ownerModId, String path) {
        SUITES.put(suiteId, new String[]{ownerModId, path});
    }

    /**
     * The icon for {@code key} ({@code HubEntry.iconKey}): a mod id, or {@code suite:<id>}. Null when
     * there is none; the caller then leaves the picture out.
     */
    public static @Nullable Icon of(@Nullable String key) {
        if (key == null) return null;
        return CACHE.computeIfAbsent(key, ModIcons::load).orElse(null);
    }

    private static Optional<Icon> load(String key) {
        if (key.startsWith("suite:")) {
            String[] at = SUITES.get(key.substring(6));
            if (at == null) return Optional.empty();
            return ModCatalog.container(at[0]).flatMap(c -> c.findPath(at[1])).flatMap(path -> read(key, path));
        }
        return ModCatalog.container(key).flatMap(c -> c.getMetadata().getIconPath(64)
                .flatMap(c::findPath)
                .flatMap(path -> read(key, path)));
    }

    private static Optional<Icon> read(String modId, Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(in);
            // Square only, as Mod Menu requires; anything else would draw stretched.
            if (image.getWidth() != image.getHeight()) {
                image.close();
                return Optional.empty();
            }
            Identifier id = Identifier.fromNamespaceAndPath("modhub",
                    "icon/" + modId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_"));   // suite:x becomes suite_x
            int size = image.getWidth();
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "Mod Hub icon " + modId, image));
            return Optional.of(new Icon(id, size));
        } catch (Exception e) {
            LOG.warn("Mod Hub: {}'s icon at {} can't be read: {}", modId, path, e.getMessage());
            return Optional.empty();
        }
    }

    /** The letters a shortcut button shows for an entry with no icon: "Hive Sight" to "HS", "Sodium" to "So". */
    public static String initials(String name) {
        String[] words = name.strip().split("\\s+");
        if (words.length >= 2) return ("" + words[0].charAt(0) + words[1].charAt(0)).toUpperCase(Locale.ROOT);
        return name.length() >= 2 ? name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1, 2) : name;
    }
}

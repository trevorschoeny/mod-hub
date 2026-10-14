package com.trevlar.modhub.catalog;

import com.trevlar.modhub.api.ModHubEntry;
import com.trevlar.modhub.api.Settings;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Every installed mod, sorted into the mod list's sidebar entries.
 *
 * <p><b>The rules</b> (design, "Decided"):
 * <ol>
 *   <li>A mod with settings (on Mod Hub, or a screen Mod Menu can open) is an entry of its own,
 *       unless it names a suite.</li>
 *   <li>Mods naming the same {@code custom.modhub.suite} share one entry, whether or not each has
 *       settings.</li>
 *   <li>A mod with no settings and no suite goes into Auxiliary: libraries, Fabric API's modules
 *       and player mods that simply have nothing to set alike.</li>
 * * </ol>
 * Order: Mod Hub first, then the rest by name, then Auxiliary.
 *
 * <p><b>Built once per session</b>, on first use, after every mod's client init has run (Mod Menu
 * works out which mods have screens in its own).
 */
public final class ModCatalog {

    private static final Logger LOG = LoggerFactory.getLogger("modhub");

    private static @Nullable Built built;

    /** The sorted mods and suites, before folders. */
    private record Built(List<HubEntry> topLevel, Map<String, HubEntry> byId, List<ModInfo> auxiliary,
                         List<ModInfo> all) {}

    private ModCatalog() {}

    /** The sidebar, top to bottom: Mod Hub, the rest by name, Auxiliary last. */
    public static List<HubEntry> entries() {
        Built b = built();
        List<HubEntry> out = new ArrayList<>(b.topLevel());
        out.sort(Comparator.comparing((HubEntry e) -> !e.id().equals("mod:modhub"))
                .thenComparing(e -> e.name().toLowerCase(java.util.Locale.ROOT)));
        out.add(new HubEntry.Auxiliary(b.auxiliary()));
        return out;
    }

    /**
     * The entry with {@code id}, or null when it isn't installed: a sidebar entry, or any single
     * mod by {@code mod:<id>}, so a mod inside a suite can have its own shortcut and key too.
     */
    public static @Nullable HubEntry find(String id) {
        HubEntry top = built().byId().get(id);
        if (top != null) return top;
        if (id.equals(HubEntry.Auxiliary.ID)) return new HubEntry.Auxiliary(built().auxiliary());
        if (id.startsWith("mod:")) {
            String modId = id.substring(4);
            for (ModInfo m : built().all()) if (m.id().equals(modId)) return new HubEntry.Single(m);
        }
        return null;
    }

    private static Built built() {
        if (built == null) built = build();
        return built;
    }

    private static Built build() {
        Map<String, Settings> settings = settingsByMod();
        boolean modMenu = FabricLoader.getInstance().isModLoaded("modmenu");

        List<ModInfo> mods = new ArrayList<>();
        Map<String, String> suiteNames = new HashMap<>();
        for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
            ModMetadata m = c.getMetadata();
            String id = m.getId();
            CustomValue.CvObject hub = customObject(m, "modhub");
            String suite = string(hub, "suite");
            String suiteName = string(hub, "suiteName");
            if (suite != null && suiteName != null) suiteNames.putIfAbsent(suite, suiteName);
            boolean mmScreen = modMenu && !settings.containsKey(id) && ModMenuBridge.hasScreen(id);
            mods.add(new ModInfo(id, m.getName(), m.getVersion().getFriendlyString(), m.getDescription(),
                    m.getAuthors().stream().map(Person::getName).toList(), links(m),
                    settings.get(id), mmScreen, suite));
        }
        mods.sort(Comparator.comparing(mi -> mi.name().toLowerCase(java.util.Locale.ROOT)));

        // Suites first, so a suite member never also stands alone.
        Map<String, List<ModInfo>> suites = new TreeMap<>();
        List<HubEntry> top = new ArrayList<>();
        List<ModInfo> aux = new ArrayList<>();
        for (ModInfo mi : mods) {
            if (mi.suiteId() != null) suites.computeIfAbsent(mi.suiteId(), s -> new ArrayList<>()).add(mi);
            else if (mi.configurable()) top.add(new HubEntry.Single(mi));
            else aux.add(mi);
        }
        suites.forEach((id, members) -> top.add(new HubEntry.Suite(id,
                suiteNames.getOrDefault(id, humanize(id)), List.copyOf(members))));

        Map<String, HubEntry> byId = new LinkedHashMap<>();
        for (HubEntry e : top) byId.put(e.id(), e);
        return new Built(List.copyOf(top), byId, List.copyOf(aux), List.copyOf(mods));
    }

    /**
     * Each mod's settings, from its {@code modhub} entrypoint. Instantiating the entry loads the
     * mod's settings class, which declares them. A broken entry is logged and skipped, so one bad
     * mod never empties the list.
     */
    private static Map<String, Settings> settingsByMod() {
        Map<String, Settings> out = new HashMap<>();
        for (EntrypointContainer<ModHubEntry> e
                : FabricLoader.getInstance().getEntrypointContainers("modhub", ModHubEntry.class)) {
            String modId = e.getProvider().getMetadata().getId();
            try {
                out.put(modId, e.getEntrypoint().settings());
            } catch (Throwable t) {
                LOG.error("Mod Hub: {}'s modhub entrypoint failed; it's listed without settings", modId, t);
            }
        }
        return out;
    }

    /** Homepage, Source and Issues from {@code contact}, then any links Mod Menu's block names. */
    private static Map<String, String> links(ModMetadata m) {
        Map<String, String> out = new LinkedHashMap<>();
        m.getContact().get("homepage").ifPresent(u -> out.put("Homepage", u));
        m.getContact().get("sources").ifPresent(u -> out.put("Source", u));
        m.getContact().get("issues").ifPresent(u -> out.put("Issues", u));
        CustomValue.CvObject mm = customObject(m, "modmenu");
        if (mm != null && mm.get("links") != null && mm.get("links").getType() == CustomValue.CvType.OBJECT) {
            for (Map.Entry<String, CustomValue> l : mm.get("links").getAsObject()) {
                if (l.getValue().getType() == CustomValue.CvType.STRING) {
                    out.putIfAbsent(humanize(l.getKey().replaceFirst("^modmenu\\.", "")), l.getValue().getAsString());
                }
            }
        }
        return out;
    }

    private static CustomValue.@Nullable CvObject customObject(ModMetadata m, String key) {
        CustomValue v = m.getCustomValue(key);
        return v != null && v.getType() == CustomValue.CvType.OBJECT ? v.getAsObject() : null;
    }

    private static @Nullable String string(CustomValue.@Nullable CvObject o, String key) {
        if (o == null) return null;
        CustomValue v = o.get(key);
        return v != null && v.getType() == CustomValue.CvType.STRING ? v.getAsString() : null;
    }

    /** {@code "my_suite"} to {@code "My suite"}, for a suite nobody named. */
    static String humanize(String id) {
        String s = id.replace('_', ' ').replace('-', ' ').replace('.', ' ').strip();
        return s.isEmpty() ? id : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** The mod's container, for its icon. */
    public static Optional<ModContainer> container(String modId) {
        return FabricLoader.getInstance().getModContainer(modId);
    }
}

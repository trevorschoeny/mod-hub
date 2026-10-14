package com.trevlar.modhub.api;

/**
 * How a mod joins Mod Hub: a class named under the {@code modhub} entrypoint in its
 * {@code fabric.mod.json}, the way Mod Menu uses {@code modmenu}. Client-only, so a dedicated server
 * never loads it; the settings it hands over are declared in common code.
 *
 * <pre>{@code
 * "entrypoints": { "modhub": ["com.example.mymod.MyModHub"] },
 * "custom": { "modhub": { "suite": "my_suite", "suiteName": "My Suite" } }
 *
 * public final class MyModHub implements ModHubEntry {
 *     public Settings settings() { return MyConfig.S; }
 * }
 * }</pre>
 *
 * <p>With nothing more, Mod Hub draws the mod's config screen: one page, every setting in
 * declaration order, named from the lang keys {@code modhub.<modid>.<key>} and described from
 * {@code modhub.<modid>.<key>.desc}. Page layouts of a mod's own come later.
 *
 * <p>The optional {@code custom.modhub} block puts the mod in a suite: mods with the same
 * {@code suite} id share one entry in the mod list and one config screen. Any member may name the
 * suite with {@code suiteName}.
 */
public interface ModHubEntry {

    /** This mod's settings, from {@code ModHub.settings(modId)}. */
    Settings settings();
}

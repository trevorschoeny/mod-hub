package com.trevlar.modhub.example.mining;

import com.trevlar.modhub.ModHub;
import com.trevlar.modhub.api.ModHubEntry;
import com.trevlar.modhub.api.Setting;
import com.trevlar.modhub.api.Settings;

/** A pretend mining mod's settings, with no lang file, to show the fallback names. Dev runtime only. */
public final class MiningHub implements ModHubEntry {

    public static final Settings S = ModHub.settings("modhub_example_mining");
    public static final Setting<Boolean> VEIN_MINE = S.bool("veinMine", true);
    public static final Setting<Integer> VEIN_SIZE = S.intRange("veinSize", 16, 1, 64);
    public static final Setting<Boolean> ORE_HIGHLIGHT = S.bool("oreHighlight", false);

    @Override
    public Settings settings() { return S; }
}

package com.trevlar.modhub.example.lumber;

import com.trevlar.modhub.ModHub;
import com.trevlar.modhub.api.ModHubEntry;
import com.trevlar.modhub.api.Setting;
import com.trevlar.modhub.api.Settings;

/** A pretend lumber mod's settings: one of each type, and a server setting. Dev runtime only. */
public final class LumberHub implements ModHubEntry {

    public enum LeafDecay { OFF, NORMAL, FAST }

    public static final Settings S = ModHub.settings("modhub_example_lumber");
    public static final Setting<Boolean> TREE_FELLING = S.bool("treeFelling", true);
    public static final Setting<Integer> MAX_LOGS = S.intRange("maxLogs", 64, 8, 256).server();
    public static final Setting<LeafDecay> LEAF_DECAY = S.choice("leafDecay", LeafDecay.NORMAL);
    public static final Setting<Boolean> REPLANT = S.bool("replantSaplings", false);

    @Override
    public Settings settings() { return S; }
}

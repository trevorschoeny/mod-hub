package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Flow;
import com.trevlar.menukit.api.panel.InsideRegion;
import com.trevlar.menukit.api.panel.Panel;
import com.trevlar.menukit.api.panel.PanelPosition;
import com.trevlar.menukit.api.panel.PanelStyle;
import com.trevlar.menukit.api.panel.ScreenPanelAdapter;
import com.trevlar.menukit.api.panel.VanillaScreenPanelAdapter;
import com.trevlar.modhub.catalog.HubEntry;
import com.trevlar.modhub.catalog.HubState;
import com.trevlar.modhub.catalog.ModCatalog;
import com.trevlar.modhub.element.ModIconButton;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The shortcut row: one square button per pinned entry, its face the mod's own icon, docked to the
 * top of the screen and wrapping when it fills the width. The same row on the pause menu and on
 * every container screen (the inventory, creative, chests, anything with slots); each entry's
 * two checkboxes in the mod list say which.
 *
 * <p>A plain MenuKit panel on the screen's top-left spot. Its buttons are fixed when declared at
 * init, so each one stands for a place in the row ({@link #PLACES} of them) and shows whichever
 * entry is in that place now, hiding when there's none.
 */
public final class ShortcutRow {

    /** Button size: MenuKit's square, with the icon 2 px in from each edge. */
    public static final int SIZE = 18;

    /** ponytail: the row's places, fixed at init. Raise it if anyone pins more. */
    private static final int PLACES = 32;

    private ShortcutRow() {}

    /** Declares both rows. Call from client init, where MenuKit takes declarations. */
    public static void register() {
        new VanillaScreenPanelAdapter(row("modhub:shortcuts_pause", HubState::pause)).on(PauseScreen.class);
        new ScreenPanelAdapter(row("modhub:shortcuts_inventory", HubState::inventory));
    }

    private static Panel row(String id, Predicate<String> shownHere) {
        Flow.Builder flow = Flow.builder().gap(2, 2);
        for (int i = 0; i < PLACES; i++) {
            int place = i;
            flow.add(ModIconButton.iconBuilder(() -> face(at(shownHere, place)))
                    .size(SIZE, SIZE)
                    .visibleWhen(() -> at(shownHere, place) != null)
                    .tooltip(() -> {
                        HubEntry e = at(shownHere, place);
                        return Component.literal(e == null ? "" : e.name());
                    })
                    .onClick(() -> {
                        HubEntry e = at(shownHere, place);
                        if (e != null) ConfigScreen.open(e, Minecraft.getInstance().gui.screen());
                    })
                    .build());
        }
        return Panel.builder(id)
                .style(PanelStyle.NONE)
                .position(PanelPosition.screenAnchor(InsideRegion.TOP_LEFT))
                .add(flow.build())
                .build()
                .opaque(false);   // only the buttons claim the screen, not the strip they sit in
    }

    /** The entry in {@code place} of this row, or null past its end. */
    private static @Nullable HubEntry at(Predicate<String> shownHere, int place) {
        List<HubEntry> row = entries(shownHere);
        return place < row.size() ? row.get(place) : null;
    }

    /**
     * This row's entries, in the player's order: pinned here, installed, and with settings.
     * ponytail: worked out per call (a few dozen times a frame on a short list). Cache on
     * HubState changes if a profile ever shows it.
     */
    private static List<HubEntry> entries(Predicate<String> shownHere) {
        List<HubEntry> out = new ArrayList<>();
        for (String id : HubState.shortcutOrder()) {
            if (!shownHere.test(id)) continue;
            HubEntry e = ModCatalog.find(id);
            if (e != null && e.configurable()) out.add(e);
        }
        return out;
    }

    private static ModIconButton.@Nullable Face face(@Nullable HubEntry e) {
        if (e == null) return null;
        // A folder shows its own name's letters, not its first mod's icon: it's the player's.
        if (e instanceof HubEntry.Folder f) return new ModIconButton.Face("", f.name());
        var mod = e.iconMod();
        return mod == null ? null : new ModIconButton.Face(mod.id(), e.name());
    }
}

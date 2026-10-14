package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.PanelElement;
import com.trevlar.menukit.api.element.TextLabel;
import com.trevlar.menukit.api.element.TextRole;
import com.trevlar.menukit.api.panel.Panel;
import com.trevlar.menukit.api.panel.PanelPosition;
import com.trevlar.menukit.api.panel.PanelStyle;

import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One modal "are you sure" for both of Mod Hub's screens (Reset to Defaults, Delete folder):
 * a title, one line of what it does, Cancel and Confirm; Escape cancels.
 *
 * <p>Hand-built, as Inventory Plus's is, because MenuKit's {@code ConfirmDialog} fixes its text
 * when built and these name what they act on when pressed. A live-text
 * {@code ConfirmDialog} in MenuKit would replace this.
 */
final class Confirm {

    private record Pending(String title, String body, Runnable action) {}

    private static @Nullable Pending pending;

    private Confirm() {}

    static void ask(String title, String body, Runnable action) {
        pending = new Pending(title, body, action);
    }

    /** The panel, for a screen's panel list; {@code id} is the screen's own. */
    static Panel panel(String id) {
        List<PanelElement> elements = List.of(
                TextLabel.builder().at(0, 0)
                        .text(() -> Component.literal(pending == null ? "" : pending.title()))
                        .role(TextRole.HEADING).build(),
                TextLabel.builder().at(0, 14)
                        .text(() -> Component.literal(pending == null ? "" : pending.body()))
                        .role(TextRole.BODY).build(),
                Button.builder().at(0, 32).size(70, 16).label(Component.literal("Cancel"))
                        .onClick(() -> pending = null).build(),
                Button.builder().at(74, 32).size(70, 16).label(Component.literal("Confirm"))
                        .onClick(() -> {
                            Pending p = pending;
                            pending = null;
                            if (p != null) p.action().run();
                        }).build());
        return Panel.builder(id)
                .elements(elements)
                .style(PanelStyle.RAISED)
                .position(PanelPosition.center())
                .build()
                .modal()
                .onEscape(() -> pending = null)
                .visibleWhen(() -> pending != null);
    }
}

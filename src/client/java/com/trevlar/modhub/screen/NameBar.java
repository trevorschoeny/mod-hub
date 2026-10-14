package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Flow;
import com.trevlar.menukit.api.element.PanelElement;
import com.trevlar.menukit.api.element.TextLabel;
import com.trevlar.menukit.api.element.TextRole;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The inventory look's name: one bar across the top of the menu panel, the name at twice size and
 * bold, centred between two spacers. Inventory Plus's {@code SettingsMenu.nameBar}, so Mod Hub's
 * screens and IP's look the same in that look. The options look shows the screen's title instead.
 */
final class NameBar {

    static final int HEIGHT = 18;
    static final int GAP = 6;

    private NameBar() {}

    static PanelElement of(String name) {
        return Flow.builder().at(0, 0).gap(0, 0)
                .add(Flow.spacer())
                .add(TextLabel.builder()
                        .text(Component.literal(name).withStyle(ChatFormatting.BOLD))
                        .role(TextRole.HEADING).scale(2f).build())
                .add(Flow.spacer())
                .build();
    }
}

package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Divider;
import com.trevlar.menukit.api.element.PanelElement;
import com.trevlar.menukit.api.element.TextLabel;
import com.trevlar.menukit.api.element.TextRole;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Lays out one page top to bottom: a running {@code y}, and rows handed out at it. Inventory Plus's
 * {@code SettingsBody}, cut to what Mod Hub's two screens use. Every row can carry a
 * {@code visibleWhen}, so one body can hold two views in the same place (a suite's member list,
 * and the member it's showing).
 *
 * <p>Rows are placed for single-line text. A label that wraps pushes the rows below it down
 * through MenuKit's panel reflow, not here.
 */
final class Body {

    static final int TEXT_ROW = 12;
    static final int CONTROL_ROW = 22;
    static final int GAP = 6;

    /** A divider's colour on the inventory look; the options look draws vanilla's separator. */
    private static final int LINE_COLOR = 0xFF8B8B8B;

    private final List<PanelElement> out = new ArrayList<>();
    private int y;
    private BooleanSupplier visible = () -> true;

    int y() { return y; }

    /** Every row added after this shows only while {@code when} holds. */
    Body when(BooleanSupplier when) {
        this.visible = when;
        return this;
    }

    /** Moves the running y, to lay a second view over the first. */
    Body at(int y) {
        this.y = y;
        return this;
    }

    Body gap(int pixels) {
        y += pixels;
        return this;
    }

    Body heading(String text) {
        out.add(TextLabel.builder().at(0, y).text(Component.literal(text).withStyle(ChatFormatting.BOLD))
                .role(TextRole.HEADING).visibleWhen(visible).build());
        y += TEXT_ROW + 2;
        return this;
    }

    Body text(Component text, TextRole role) {
        out.add(TextLabel.builder().at(0, y).text(text).role(role).visibleWhen(visible).build());
        y += TEXT_ROW;
        return this;
    }

    Body rule() {
        out.add(Divider.horizontal().at(0, y).color(LINE_COLOR).visibleWhen(visible).build());
        y += GAP;
        return this;
    }

    /** Builds a row's elements at {@code y}, each given {@code visible} as its {@code visibleWhen}. */
    interface Row {
        List<PanelElement> at(int y, BooleanSupplier visible);
    }

    /** Any row: {@code row} builds its elements; {@code height} is the room it takes. */
    Body row(Row row, int height) {
        out.addAll(row.at(y, visible));
        y += height;
        return this;
    }

    List<PanelElement> build() {
        return List.copyOf(out);
    }
}

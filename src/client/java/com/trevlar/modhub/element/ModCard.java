package com.trevlar.modhub.element;

import com.trevlar.menukit.api.element.AbstractPanelElement;
import com.trevlar.menukit.api.element.RenderContext;
import com.trevlar.menukit.api.element.Section;
import com.trevlar.menukit.api.element.TextRole;
import com.trevlar.menukit.api.panel.ScreenKind;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * One mod as a box on a suite's or Auxiliary's page: a frame the width of the page with the mod's
 * icon, its name in bold and its version. The page lays the rest over it, inside the frame: the
 * Description section ({@link #SECTION_Y}) and the controls row ({@link #controlsY}).
 *
 * <p>The box grows with its Description: {@code extra} is the section's open height, read every
 * frame, and the panel's reflow pushes everything after the section down by the same amount, so
 * the next box moves with this one's bottom edge.
 *
 * <p>ponytail: Mod Hub's own box; MenuKit has no public grouping box. Ask for one if a second
 * mod wants it.
 */
public final class ModCard extends AbstractPanelElement {

    /** Inside the frame: its padding, and the icon's size. */
    public static final int PAD = 4;
    private static final int ICON = 16;

    /** Where the Description section sits, from the card's top. */
    public static final int SECTION_Y = PAD + ICON + 4;

    /** The controls: one row as tall as its icon buttons. */
    public static final int CONTROLS_HEIGHT = 19;

    private final String modId;
    private final String name;
    private final boolean hasIcon;
    private final String version;
    private final int baseHeight;
    private final IntSupplier extra;
    private final BooleanSupplier visible;

    /**
     * @param description whether there's a Description section (a mod with none has no row for it)
     * @param controls    whether there's a controls row (a mod with no settings has none)
     * @param extra       how much taller the Description section is than its header right now
     */
    public ModCard(int y, String modId, String name, String version, boolean description, boolean controls,
                   IntSupplier extra, BooleanSupplier visible) {
        this.childX = 0;
        this.childY = y;
        this.modId = modId;
        this.name = name;
        this.hasIcon = com.trevlar.modhub.catalog.ModIcons.of(modId) != null;
        this.version = version;
        this.baseHeight = height(description, controls);
        this.height = baseHeight;
        this.extra = extra;
        this.visible = visible;
    }

    /** Where the controls row sits, from the card's top, with its section closed. */
    public static int controlsY(boolean description) {
        return description ? SECTION_Y + Section.HEADER_HEIGHT + 4 : SECTION_Y;
    }

    /** The card's height with its section closed: what the page leaves for it. */
    public static int height(boolean description, boolean controls) {
        if (controls) return controlsY(description) + CONTROLS_HEIGHT + PAD;
        return description ? SECTION_Y + Section.HEADER_HEIGHT + PAD : PAD + ICON + PAD;
    }

    @Override public boolean isVisible() { return visible.getAsBoolean(); }

    @Override public int getHeight() { return baseHeight + extra.getAsInt(); }

    /** As wide as the page. */
    @Override public void fillWidth(int width) { this.width = width; }
    @Override public void layoutWithin(int budget) { this.width = budget; }
    @Override public int naturalWidth() { return 0; }

    @Override
    public void render(RenderContext ctx) {
        int x = ctx.originX() + childX, y = ctx.originY() + childY, w = width, h = getHeight();
        var g = ctx.graphics();
        // A quiet frame on both looks: darker than the page on the options look, a grey line on
        // the inventory's light panel.
        boolean options = ctx.kind() == ScreenKind.OPTIONS;
        g.fill(x, y, x + w, y + h, options ? 0x40000000 : 0x14000000);
        g.outline(x, y, w, h, options ? 0x30FFFFFF : 0xFF8B8B8B);

        // No icon: no picture, and the name moves left into its place.
        ModIconImage.draw(ctx, com.trevlar.modhub.catalog.ModIcons.of(modId), x + PAD, y + PAD, ICON);

        var font = Minecraft.getInstance().font;
        boolean shadow = ctx.kind().textShadow();
        int tx = x + PAD + (hasIcon ? ICON + 6 : 0), ty = y + PAD + (ICON - 8) / 2, right = x + w - PAD;
        Component title = Component.literal(name).withStyle(ChatFormatting.BOLD);
        g.text(font, title, tx, ty, ctx.kind().textColor(TextRole.HEADING), shadow);
        int vx = tx + font.width(title) + 6;
        if (!version.isEmpty() && vx < right) {
            g.text(font, font.plainSubstrByWidth(version, right - vx), vx, ty, ctx.kind().textColor(TextRole.MUTED), shadow);
        }
    }
}

package com.trevlar.modhub.element;

import com.trevlar.menukit.api.element.AbstractPanelElement;
import com.trevlar.menukit.api.element.RenderContext;
import com.trevlar.menukit.api.element.TextRole;
import com.trevlar.menukit.api.panel.ScreenKind;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * One mod (or a suite inside a folder) as a box on a suite's, folder's or Auxiliary's page: a frame
 * the width of the page, the icon at the left, the name in bold with its version after it, and one
 * line of description, cut short with "..." when it runs out of width. Render only: the page lays
 * the mod's controls (configs, key, shortcut) over the box's lower half, at {@link #CONTROLS_Y}.
 *
 * <p>The height is fixed so the rows after it never move, which is why the description is one
 * line. ponytail: Mod Hub's own box; MenuKit has no public grouping box. Ask for one if a second
 * mod wants it.
 */
public final class ModCard extends AbstractPanelElement {

    /** Room for the icon and two lines, then the controls row, with padding around both. */
    public static final int HEIGHT_WITH_CONTROLS = 52;
    public static final int HEIGHT_PLAIN = 30;
    /** Where the controls row starts, from the card's top, and from its left (in line with the text). */
    public static final int CONTROLS_Y = 30;
    public static final int TEXT_X = 30;

    private static final int PAD = 4;
    private static final int ICON = 20;

    private final String modId;
    private final String name;
    private final String version;
    private final String description;
    private final BooleanSupplier visible;

    public ModCard(int y, boolean withControls, String modId, String name, String version, String description,
                   BooleanSupplier visible) {
        this.childX = 0;
        this.childY = y;
        this.height = withControls ? HEIGHT_WITH_CONTROLS : HEIGHT_PLAIN;
        this.modId = modId;
        this.name = name;
        this.version = version;
        this.description = description;
        this.visible = visible;
    }

    @Override public boolean isVisible() { return visible.getAsBoolean(); }

    /** As wide as the page. */
    @Override public void fillWidth(int width) { this.width = width; }
    @Override public void layoutWithin(int budget) { this.width = budget; }
    @Override public int naturalWidth() { return 0; }

    @Override
    public void render(RenderContext ctx) {
        int x = ctx.originX() + childX, y = ctx.originY() + childY, w = width, h = height;
        var g = ctx.graphics();
        // A quiet frame on both looks: darker than the page on the options look, a grey line on
        // the inventory's light panel.
        boolean options = ctx.kind() == ScreenKind.OPTIONS;
        g.fill(x, y, x + w, y + h, options ? 0x40000000 : 0x14000000);
        g.outline(x, y, w, h, options ? 0x30FFFFFF : 0xFF8B8B8B);

        ModIconImage.draw(ctx, modId, name, x + PAD + 1, y + PAD + 1, ICON);

        var font = Minecraft.getInstance().font;
        boolean shadow = ctx.kind().textShadow();
        int tx = x + TEXT_X, right = x + w - PAD;
        Component title = Component.literal(name).withStyle(ChatFormatting.BOLD);
        g.text(font, title, tx, y + PAD + 1, ctx.kind().textColor(TextRole.HEADING), shadow);
        int vx = tx + font.width(title) + 6;
        if (!version.isEmpty() && vx < right) {
            g.text(font, font.plainSubstrByWidth(version, right - vx), vx, y + PAD + 1,
                    ctx.kind().textColor(TextRole.MUTED), shadow);
        }
        if (!description.isBlank()) {
            g.text(font, oneLine(font, description, right - tx), tx, y + PAD + 13,
                    ctx.kind().textColor(TextRole.MUTED), shadow);
        }
    }

    /** {@code text} cut to {@code width}, ending in "..." when it was cut. */
    private static String oneLine(net.minecraft.client.gui.Font font, String text, int width) {
        String flat = text.replace('\n', ' ');
        if (font.width(flat) <= width) return flat;
        return font.plainSubstrByWidth(flat, Math.max(0, width - font.width("..."))).stripTrailing() + "...";
    }
}

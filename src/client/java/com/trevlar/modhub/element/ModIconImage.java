package com.trevlar.modhub.element;

import com.trevlar.menukit.api.element.AbstractPanelElement;
import com.trevlar.menukit.api.element.RenderContext;
import com.trevlar.menukit.api.element.TextRole;
import com.trevlar.modhub.catalog.ModIcons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.function.BooleanSupplier;

/**
 * A mod's icon at a fixed size, drawn from its texture ({@link ModIcons}), or its initials when it
 * has none. No input. The mod list's big icon and its tiny-icon preview.
 */
public final class ModIconImage extends AbstractPanelElement {

    private final String modId;
    private final String name;
    private final BooleanSupplier visible;

    public ModIconImage(int x, int y, int size, String modId, String name, BooleanSupplier visible) {
        this.childX = x;
        this.childY = y;
        this.width = size;
        this.height = size;
        this.authoredWidth = size;
        this.modId = modId;
        this.name = name;
        this.visible = visible;
    }

    public ModIconImage(int x, int y, int size, String modId, String name) {
        this(x, y, size, modId, name, () -> true);
    }

    @Override public boolean isVisible() { return visible.getAsBoolean(); }

    @Override public void layoutWithin(int budget) {}
    @Override public void fillWidth(int width) {}

    @Override
    public void render(RenderContext ctx) {
        draw(ctx, modId, name, ctx.originX() + childX, ctx.originY() + childY, width);
    }

    /** Draws {@code modId}'s icon as a {@code size}-pixel square at {@code (x, y)}, or its initials. */
    static void draw(RenderContext ctx, String modId, String name, int x, int y, int size) {
        ModIcons.Icon icon = ModIcons.of(modId);
        if (icon != null) {
            int s = icon.size();
            ctx.graphics().blit(RenderPipelines.GUI_TEXTURED, icon.texture(), x, y, 0f, 0f, size, size, s, s, s, s);
            return;
        }
        var font = Minecraft.getInstance().font;
        String letters = ModIcons.initials(name);
        int color = ctx.kind().textColor(TextRole.HEADING);
        ctx.graphics().text(font, letters, x + (size - font.width(letters)) / 2, y + (size - 8) / 2, color,
                ctx.kind().textShadow());
    }
}

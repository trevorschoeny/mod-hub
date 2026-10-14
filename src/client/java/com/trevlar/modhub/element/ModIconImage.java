package com.trevlar.modhub.element;

import com.trevlar.menukit.api.element.AbstractPanelElement;
import com.trevlar.menukit.api.element.RenderContext;
import com.trevlar.modhub.catalog.ModIcons;

import net.minecraft.client.renderer.RenderPipelines;

import java.util.function.BooleanSupplier;

/**
 * An icon at a fixed size, drawn from its texture ({@link ModIcons}): a mod's, or a suite's. No
 * input. A page only adds one when the icon exists, so nothing stands in for a missing one.
 */
public final class ModIconImage extends AbstractPanelElement {

    private final String key;
    private final BooleanSupplier visible;

    public ModIconImage(int x, int y, int size, String key, BooleanSupplier visible) {
        this.childX = x;
        this.childY = y;
        this.width = size;
        this.height = size;
        this.authoredWidth = size;
        this.key = key;
        this.visible = visible;
    }

    @Override public boolean isVisible() { return visible.getAsBoolean(); }

    @Override public void layoutWithin(int budget) {}
    @Override public void fillWidth(int width) {}

    @Override
    public void render(RenderContext ctx) {
        draw(ctx, ModIcons.of(key), ctx.originX() + childX, ctx.originY() + childY, width);
    }

    /** Draws {@code icon} as a {@code size}-pixel square at {@code (x, y)}; nothing when it's null. */
    static void draw(RenderContext ctx, ModIcons.@org.jetbrains.annotations.Nullable Icon icon, int x, int y, int size) {
        if (icon == null) return;
        int s = icon.size();
        ctx.graphics().blit(RenderPipelines.GUI_TEXTURED, icon.texture(), x, y, 0f, 0f, size, size, s, s, s, s);
    }
}

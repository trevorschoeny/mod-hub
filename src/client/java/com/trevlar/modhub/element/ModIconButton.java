package com.trevlar.modhub.element;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.RenderContext;
import com.trevlar.modhub.catalog.ModIcons;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A square button whose face is a mod's icon: a shortcut. Which mod is read every frame, so one
 * button stands for whichever entry is in its place in the shortcut row right now. MenuKit's
 * {@code Button} draws the background, the press and the tooltip; this draws only the face.
 */
public final class ModIconButton extends Button {

    /**
     * What the face shows: the icon for {@code iconKey}, or the name's initials when there's none,
     * since a shortcut has to show something to be told apart.
     */
    public record Face(@Nullable String iconKey, String name) {}

    /** The icon's inset from the button's edge. */
    private static final int INSET = 2;

    private final Supplier<@Nullable Face> face;

    private ModIconButton(Builder b) {
        super(b);
        this.face = b.face;
    }

    public static Builder iconBuilder(Supplier<@Nullable Face> face) {
        return new Builder(face);
    }

    @Override public void fillWidth(int width) {}
    @Override public void layoutWithin(int budget) {}

    @Override
    protected void renderContent(RenderContext ctx, int sx, int sy, Look look) {
        Face f = face.get();
        if (f == null) return;
        int size = getWidth() - INSET * 2;
        ModIcons.Icon icon = ModIcons.of(f.iconKey());
        if (icon != null) {
            ModIconImage.draw(ctx, icon, sx + INSET, sy + INSET, size);
            return;
        }
        var font = net.minecraft.client.Minecraft.getInstance().font;
        String letters = ModIcons.initials(f.name());
        ctx.graphics().text(font, letters, sx + (getWidth() - font.width(letters)) / 2, sy + (getHeight() - 8) / 2,
                ctx.kind().textColor(com.trevlar.menukit.api.element.TextRole.HEADING), ctx.kind().textShadow());
    }

    public static final class Builder extends Button.Builder {
        private final Supplier<@Nullable Face> face;

        private Builder(Supplier<@Nullable Face> face) {
            this.face = face;
        }

        @Override
        public Button build() {
            return new ModIconButton(this);
        }
    }
}

package com.trevlar.modhub.element;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.RenderContext;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A square button whose face is a mod's icon: a shortcut. Which mod is read every frame, so one
 * button stands for whichever entry is in its place in the shortcut row right now. MenuKit's
 * {@code Button} draws the background, the press and the tooltip; this draws only the face.
 */
public final class ModIconButton extends Button {

    /** What the face shows: a mod's icon, or its name's initials when it has none. */
    public record Face(String modId, String name) {}

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
        ModIconImage.draw(ctx, f.modId(), f.name(), sx + INSET, sy + INSET, getWidth() - INSET * 2);
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

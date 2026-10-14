package com.trevlar.modhub.element;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.ElementConstants;
import com.trevlar.menukit.api.element.RenderContext;
import com.trevlar.menukit.api.element.Text;
import com.trevlar.menukit.api.element.TextRole;
import com.trevlar.modhub.catalog.HubState;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

/**
 * The button that sets an entry's key. Click it and it waits ("> Press a key <"); the next key
 * pressed on the screen binds, Escape cancels, Backspace or Delete unbinds. Right-click unbinds
 * too. The screen hands it the key press ({@link #keyPressed}), since only the screen sees every key.
 *
 * <p>Mod Hub's own bindings, not vanilla {@code KeyMapping}s: vanilla wants every key registered
 * at startup, which would put one empty entry per mod in Controls (design, Decided).
 */
public final class KeyButton extends Button {

    /** The entry waiting for a key, on any open list screen, or null. */
    private static @Nullable String waiting;

    private final String entry;

    private KeyButton(Builder b, String entry) {
        super(b);
        this.entry = entry;
    }

    public static Button of(int x, int y, String entry) {
        Builder b = new Builder(entry);
        b.at(x, y).size(110, 16)
                .onClick(() -> waiting = entry)
                .onSecondaryClick(click -> { HubState.setKey(entry, null); waiting = null; })
                .tooltip(Component.literal("Click, then press a key.\nBackspace or right-click clears it."));
        return b.build();
    }

    /**
     * A key pressed on the screen: binds it to the waiting entry and answers true, or answers
     * false when nothing is waiting.
     */
    public static boolean keyPressed(int keyCode) {
        String entry = waiting;
        if (entry == null) return false;
        waiting = null;
        if (keyCode == InputConstants.KEY_ESCAPE) return true;
        if (keyCode == InputConstants.KEY_BACKSPACE || keyCode == InputConstants.KEY_DELETE) {
            HubState.setKey(entry, null);
        } else {
            HubState.setKey(entry, keyCode);
        }
        return true;
    }

    /** Stops waiting, when the screen closes. */
    public static void cancel() {
        waiting = null;
    }

    /** {@code code}'s name as the Controls screen shows it ("K", "Left Shift"). */
    public static Component keyName(int code) {
        return InputConstants.Type.KEYBOARD.getOrCreate(code).getDisplayName();
    }

    @Override
    protected void renderContent(RenderContext ctx, int sx, int sy, Look look) {
        Component label;
        if (entry.equals(waiting)) {
            label = Component.literal("> Press a key <").withStyle(ChatFormatting.YELLOW);
        } else {
            Integer key = HubState.key(entry);
            label = key == null ? Component.literal("Not bound") : keyName(key);
        }
        int color = look.disabled() ? ctx.kind().textColor(TextRole.DISABLED) : ElementConstants.TEXT_LIGHT;
        Text.renderCentered(ctx.graphics(), label, sx, sy, getWidth(), getHeight(), color, true);
    }

    private static final class Builder extends Button.Builder {
        private final String entry;

        Builder(String entry) {
            this.entry = entry;
        }

        @Override
        public Button build() {
            return new KeyButton(this, entry);
        }
    }
}

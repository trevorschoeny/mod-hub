package com.trevlar.modhub.page;

import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * One mod's settings as the config screen draws them, whatever made them: Mod Hub's own settings,
 * or another library's config translated (see {@code compat}). The screen draws only this, so every
 * mod's page looks the same and a control is drawn one way everywhere.
 *
 * @param categories the library's own tabs (a YACL or Cloth category); one, unnamed, for a mod on
 *                   Mod Hub
 * @param resetAll   what Reset to Defaults runs, or null when the page has nothing to reset
 */
public record ConfigPage(List<Category> categories, @Nullable Runnable resetAll) {

    /** A run of blocks under a name. {@code name} null for a page with one category and no title. */
    public record Category(@Nullable Component name, List<Block> blocks) {}

    public sealed interface Block permits Row, Group {}

    /**
     * One setting.
     *
     * @param description shown when the name is hovered
     * @param note        a short line under it, such as "Needs a restart"
     */
    public record Row(Component name, @Nullable Component description, @Nullable Component note,
                      Control control) implements Block {}

    /** A named, collapsible run of rows (a YACL or Cloth group). */
    public record Group(Component name, @Nullable Component description, boolean startOpen, List<Row> rows)
            implements Block {}

    /** Every row on the page, groups included, in order. */
    public Stream<Row> rows() {
        return categories.stream().flatMap(c -> c.blocks().stream())
                .flatMap(b -> b instanceof Group g ? g.rows().stream() : Stream.of((Row) b));
    }

    /** Whether the screen can draw every row; false sends the mod to its original screen. */
    public boolean drawable() {
        return rows().noneMatch(r -> r.control() instanceof Control.Unsupported);
    }
}

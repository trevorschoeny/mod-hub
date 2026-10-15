package com.trevlar.modhub.page;

import com.trevlar.modhub.api.Setting;
import com.trevlar.modhub.api.Settings;

import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A mod on Mod Hub, as a page: one unnamed category, every setting in declaration order, names and
 * descriptions from the mod's lang keys. The first of the translators; the others are in
 * {@code compat}.
 */
public final class NativePage {

    private NativePage() {}

    public static ConfigPage of(Settings settings) {
        List<ConfigPage.Block> rows = new ArrayList<>();
        for (Setting<?> s : settings.all()) rows.add(row(s));
        return new ConfigPage(List.of(new ConfigPage.Category(null, rows)), settings::resetAll);
    }

    private static ConfigPage.Row row(Setting<?> s) {
        // Each setting reads and writes itself: set() checks the value, saves the file and tells
        // the mod's listeners.
        Control control = switch (s) {
            case Setting.Bool b -> new Control.Toggle(Value.of(b::get, b::set, b.defaultValue()));
            case Setting.Int i -> new Control.IntSlider(Value.of(i::get, i::set, i.defaultValue()), i.min, i.max, 1);
            case Setting.Choice<?> c -> choice(c);
        };
        return new ConfigPage.Row(Labels.name(s), tooltip(s), null, control);
    }

    private static <E extends Enum<E>> Control choice(Setting.Choice<E> c) {
        return new Control.Choice<>(Value.of(c::get, c::set, c.defaultValue()), c.values(), e -> Labels.value(c, e));
    }

    /** The setting's description, and for a server setting, who decides it. Null when neither. */
    private static @Nullable Component tooltip(Setting<?> s) {
        Component desc = Labels.description(s);
        if (!s.isServerSide()) return desc;
        Component server = Component.literal("On a server, the server's own setting decides.");
        return desc == null ? server : Component.empty().append(desc).append("\n").append(server);
    }
}

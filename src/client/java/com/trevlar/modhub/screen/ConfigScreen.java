package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.Checkbox;
import com.trevlar.menukit.api.element.Dropdown;
import com.trevlar.menukit.api.element.PanelElement;
import com.trevlar.menukit.api.element.Slider;
import com.trevlar.menukit.api.element.Tabs;
import com.trevlar.menukit.api.element.TextRole;
import com.trevlar.menukit.api.panel.MKScreen;
import com.trevlar.menukit.api.panel.OutsideRegion;
import com.trevlar.menukit.api.panel.Panel;
import com.trevlar.menukit.api.panel.PanelPosition;
import com.trevlar.menukit.api.panel.PanelStyle;
import com.trevlar.menukit.api.panel.ScreenKind;
import com.trevlar.modhub.ModHubSettings;
import com.trevlar.modhub.api.Setting;
import com.trevlar.modhub.catalog.HubEntry;
import com.trevlar.modhub.catalog.ModInfo;
import com.trevlar.modhub.catalog.ModMenuBridge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A mod's, suite's or folder's settings: Inventory Plus's settings menu made general. Sidebar tabs
 * on the left, one per mod with settings; the page on the right; Done at the bottom; Escape back to
 * wherever it was opened from (the mod list, the pause menu, a container).
 *
 * <p>Each page is drawn from the mod's declared settings, in declaration order, under IP's frame:
 * the mod's name, its description, Reset to Defaults, the rule, then one row per setting. A mod
 * whose screen is Mod Menu's (YACL and others) gets a page with one button that opens it.
 *
 * <p>Nothing to save: every control writes its setting on change.
 */
public final class ConfigScreen extends MKScreen {

    /** The open tab per entry, for the session. */
    private static final Map<String, String> SELECTED = new HashMap<>();
    private static final Map<String, Double> SCROLL = new HashMap<>();

    private ConfigScreen(HubEntry entry, @Nullable Screen parent, ScreenKind kind) {
        super(Component.literal(entry.name()), kind,
                List.of(main(entry), done(), Confirm.panel("modhub:config_confirm")));
        setReturnAction(() -> Minecraft.getInstance().gui.setScreen(parent));
    }

    /**
     * Opens {@code entry}'s settings over {@code parent}. A lone mod whose screen is Mod Menu's
     * opens that screen directly, since a page with one button would only be in the way.
     */
    public static void open(HubEntry entry, @Nullable Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        List<ModInfo> mods = entry.mods().stream().filter(ModInfo::configurable).toList();
        if (mods.size() == 1 && mods.getFirst().settings() == null) {
            Screen own = ModMenuBridge.screen(mods.getFirst().id(), parent);
            if (own != null) {
                mc.gui.setScreen(own);
                return;
            }
        }
        ScreenKind kind = ModHubSettings.OPTIONS_LOOK.get() ? ScreenKind.OPTIONS : ScreenKind.CONTAINER;
        mc.gui.setScreen(new ConfigScreen(entry, parent, kind));
    }

    private static Panel main(HubEntry entry) {
        Tabs.Builder tabs = Tabs.builder()
                .mode(Tabs.Mode.SIDEBAR).align(Tabs.Align.LEFT)
                .state(() -> SELECTED.get(entry.id()), id -> SELECTED.put(entry.id(), id))
                .bodyScroll(id -> SCROLL.get(entry.id() + "/" + id), (id, v) -> SCROLL.put(entry.id() + "/" + id, v));
        for (ModInfo mod : entry.mods()) {
            if (!mod.configurable()) continue;
            tabs.tab(Tabs.tab(mod.id()).label(Component.literal(mod.name())).body(() -> page(mod)));
        }
        return Panel.builder("modhub:config")
                .style(PanelStyle.NONE)
                .position(PanelPosition.main())
                .add(tabs.build())
                .build();
    }

    /** Done at the bottom centre, as every Options screen ends. Done and Escape agree. */
    private static Panel done() {
        return Panel.builder("modhub:config_done")
                .style(PanelStyle.NONE)
                .position(PanelPosition.region(OutsideRegion.BOTTOM_CENTER))
                .add(Button.builder().label(CommonComponents.GUI_DONE).size(200, 20)
                        .onClick(() -> Minecraft.getInstance().gui.screen().onClose()).build())
                .build();
    }

    // One mod's page

    private static List<PanelElement> page(ModInfo mod) {
        Body body = new Body();
        body.heading(mod.name());
        if (!mod.description().isBlank()) body.text(Component.literal(mod.description()), TextRole.BODY);
        body.gap(4);

        if (mod.settings() == null) {
            // A Mod Menu screen: Mod Hub can open it but not draw it.
            body.row((y, v) -> List.of(Button.builder().at(0, y).size(120, 20)
                    .label(Component.literal("Open settings"))
                    .onClick(() -> {
                        Screen here = Minecraft.getInstance().gui.screen();
                        Screen own = ModMenuBridge.screen(mod.id(), here);
                        if (own != null) Minecraft.getInstance().gui.setScreen(own);
                    }).build()), Body.CONTROL_ROW);
            body.text(Component.literal("This mod's settings are its own screen, through Mod Menu."), TextRole.MUTED);
            return body.build();
        }

        body.row((y, v) -> List.of(Button.builder().at(0, y).size(0, 16)
                .label(Component.literal("Reset to Defaults"))
                .onClick(() -> Confirm.ask("Reset " + mod.name() + "?",
                        "Every setting on this page goes back to its default.", mod.settings()::resetAll))
                .build()), Body.CONTROL_ROW - 2);
        body.rule();

        for (Setting<?> s : mod.settings().all()) row(body, s);
        return body.build();
    }

    /** One setting: a checkbox inline, or its name above a slider or dropdown. */
    private static void row(Body body, Setting<?> s) {
        Component desc = Labels.description(s);
        switch (s) {
            case Setting.Bool b -> body.row((y, v) -> {
                Checkbox.Builder c = Checkbox.builder().at(0, y).label(Labels.name(b))
                        .state(b::get, b::set);
                if (desc != null) c.tooltip(desc);
                return List.of(c.build());
            }, Body.TEXT_ROW + 6);
            case Setting.Int i -> {
                body.text(Labels.name(i), TextRole.BODY);
                body.row((y, v) -> List.of(Slider.ofInts(i.min, i.max).at(0, y).size(180, 16)
                        .state(i::get, i::set).build()), Body.CONTROL_ROW);
            }
            case Setting.Choice<?> c -> choice(body, c);
        }
        if (desc != null && !(s instanceof Setting.Bool)) {
            body.text(desc, TextRole.MUTED);
        }
        if (s.isServerSide()) {
            body.text(Component.literal("On a server, the server's own setting decides."), TextRole.MUTED);
        }
        body.gap(2);
    }

    private static <E extends Enum<E>> void choice(Body body, Setting.Choice<E> c) {
        body.text(Labels.name(c), TextRole.BODY);
        body.row((y, v) -> List.of(Dropdown.<E>builder().at(0, y).size(120, 16)
                .items(c.values())
                .label(e -> Labels.value(c, e))
                .state(c::get, c::set)
                .build()), Body.CONTROL_ROW);
    }
}

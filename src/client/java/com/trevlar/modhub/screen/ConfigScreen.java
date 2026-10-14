package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.Checkbox;
import com.trevlar.menukit.api.element.Dropdown;
import com.trevlar.menukit.api.element.Flow;
import com.trevlar.menukit.api.element.PanelElement;
import com.trevlar.menukit.api.element.Slider;
import com.trevlar.menukit.api.element.Tabs;
import com.trevlar.menukit.api.element.TextRole;
import com.trevlar.menukit.api.panel.InsideRegion;
import com.trevlar.menukit.api.panel.MKScreen;
import com.trevlar.menukit.api.panel.OutsideRegion;
import com.trevlar.menukit.api.panel.Panel;
import com.trevlar.menukit.api.panel.PanelPosition;
import com.trevlar.menukit.api.panel.PanelStyle;
import com.trevlar.menukit.api.panel.ScreenKind;
import com.trevlar.modhub.ModHubSettings;
import com.trevlar.modhub.api.Setting;
import com.trevlar.modhub.catalog.HubEntry;
import com.trevlar.modhub.catalog.ModCatalog;
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

    /** How far the menu starts below the top corners' buttons (Trev, 2026-10-14: 6 or 7 px). */
    private static final int TOP_ROOM = 7;

    private ConfigScreen(HubEntry entry, @Nullable Screen parent, ScreenKind kind) {
        super(Component.literal(entry.name()), kind,
                List.of(main(entry, kind), done(), back(), modHub(parent), Confirm.panel("modhub:config_confirm")));
        // The inventory look names the screen in its own bar inside the panel, as IP's does.
        if (kind == ScreenKind.CONTAINER) hideTitle();
        setReturnAction(() -> Minecraft.getInstance().gui.setScreen(parent));
    }

    /**
     * Opens {@code entry}'s settings over {@code parent}. A mod in a suite opens the suite's screen
     * on that mod's tab. A lone mod whose screen is Mod Menu's opens that screen directly, since a
     * page with one button would only be in the way.
     */
    public static void open(HubEntry entry, @Nullable Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        // A mod in a suite opens in its suite's screen, on its own tab, so the suite stays together.
        if (entry instanceof HubEntry.Single one && one.mod().suiteId() != null) {
            HubEntry suite = ModCatalog.find("suite:" + one.mod().suiteId());
            if (suite != null) {
                SELECTED.put(suite.id(), one.mod().id());
                entry = suite;
            }
        }
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

    private static Panel main(HubEntry entry, ScreenKind kind) {
        boolean options = kind == ScreenKind.OPTIONS;
        Tabs.Builder tabs = Tabs.builder()
                .mode(Tabs.Mode.SIDEBAR).align(Tabs.Align.LEFT)
                .state(() -> SELECTED.get(entry.id()), id -> SELECTED.put(entry.id(), id))
                .bodyScroll(id -> SCROLL.get(entry.id() + "/" + id), (id, v) -> SCROLL.put(entry.id() + "/" + id, v));
        for (ModInfo mod : entry.mods()) {
            if (!mod.configurable()) continue;
            tabs.tab(Tabs.tab(mod.id()).label(Component.literal(mod.name())).body(() -> page(mod)));
        }
        // Back and Mod Hub sit in the top corners; everything here starts TOP_ROOM lower so they
        // never overlap the sidebar or the name bar.
        tabs.at(0, TOP_ROOM + (options ? 0 : NameBar.HEIGHT + NameBar.GAP));
        Panel.Builder panel = Panel.builder("modhub:config")
                .style(options ? PanelStyle.NONE : PanelStyle.RAISED)
                .position(PanelPosition.main());
        if (!options) panel.add(NameBar.of(entry.name(), TOP_ROOM));
        return panel.add(tabs.build()).build();
    }

    /** Back, top left: where the screen was opened from, as Escape and Done go. */
    private static Panel back() {
        return Panel.builder("modhub:config_back")
                .style(PanelStyle.NONE)
                .position(PanelPosition.screenAnchor(InsideRegion.TOP_LEFT))
                .add(Button.builder().label(Component.literal("< Back")).size(50, 20)
                        .onClick(() -> Minecraft.getInstance().gui.screen().onClose()).build())
                .build();
    }

    /**
     * The Mod Hub button, top right: the pause menu's own (same sprite, same tooltip), opening the
     * mod list. Opened from the list, it goes back to that list rather than stacking a second one.
     */
    private static Panel modHub(@Nullable Screen parent) {
        return Panel.builder("modhub:config_modhub")
                .style(PanelStyle.NONE)
                .position(PanelPosition.screenAnchor(InsideRegion.TOP_RIGHT))
                .add(Button.builder().icon(ModListScreen.MODS_SPRITE).size(20, 20)
                        .tooltip(Component.literal("Mod Hub"))
                        .onClick(() -> {
                            if (parent instanceof ModListScreen) Minecraft.getInstance().gui.setScreen(parent);
                            else ModListScreen.open(parent);
                        }).build())
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
    //
    // Inventory Plus's frame and spacing (SettingsBody), so a Mod Hub page and an IP tab read the
    // same: the title in bold, the description, Reset to Defaults on its own row, the rule, then
    // the settings one per line. A checkbox sits inline with its name; a slider or dropdown sits
    // under its name. A setting's description is its name's tooltip, as IP's keys' are.

    private static List<PanelElement> page(ModInfo mod) {
        Body body = new Body();
        body.heading(mod.name());
        body.text(Component.literal(mod.description().isBlank() ? " " : mod.description()), TextRole.BODY);
        body.gap(4);

        if (mod.settings() == null) {
            // A Mod Menu screen: Mod Hub can open it but not draw it.
            body.row((y, v) -> List.of(Button.builder().at(0, y).size(0, 16)
                    .label(Component.literal("Open settings"))
                    .tooltip(Component.literal("This mod's settings are its own screen, through Mod Menu."))
                    .onClick(() -> {
                        Screen here = Minecraft.getInstance().gui.screen();
                        Screen own = ModMenuBridge.screen(mod.id(), here);
                        if (own != null) Minecraft.getInstance().gui.setScreen(own);
                    }).build()), Body.CONTROL_ROW);
            return body.build();
        }

        body.row((y, v) -> List.of(Flow.builder().at(0, y).gap(10, 4)
                .add(Button.builder().size(0, 16).label(Component.literal("Reset to Defaults"))
                        .onClick(() -> Confirm.ask("Reset " + mod.name() + " to defaults?",
                                "Every setting on this tab goes back to how a fresh install has it.",
                                mod.settings()::resetAll))
                        .build())
                .build()), Body.CONTROL_ROW);
        body.rule();

        for (Setting<?> s : mod.settings().all()) row(body, s);
        return body.build();
    }

    /** One setting: a checkbox inline with its name, or its name above a slider or dropdown. */
    private static void row(Body body, Setting<?> s) {
        Component tip = tooltip(s);
        switch (s) {
            case Setting.Bool b -> body.row((y, v) -> {
                Checkbox.Builder c = Checkbox.builder().at(0, y).label(Labels.name(b)).state(b::get, b::set);
                if (tip != null) c.tooltip(tip);
                return List.of(c.build());
            }, Body.TEXT_ROW + 4);
            case Setting.Int i -> {
                body.text(Labels.name(i), TextRole.BODY, tip);
                body.row((y, v) -> List.of(Slider.ofInts(i.min, i.max).at(0, y).size(180, 16)
                        .state(i::get, i::set).build()), Body.CONTROL_ROW);
            }
            case Setting.Choice<?> c -> choice(body, c, tip);
        }
    }

    /** The setting's description, and for a server setting, who decides it. Null when neither. */
    private static @Nullable Component tooltip(Setting<?> s) {
        Component desc = Labels.description(s);
        if (!s.isServerSide()) return desc;
        Component server = Component.literal("On a server, the server's own setting decides.");
        return desc == null ? server : Component.empty().append(desc).append("\n").append(server);
    }

    private static <E extends Enum<E>> void choice(Body body, Setting.Choice<E> c, @Nullable Component tip) {
        body.text(Labels.name(c), TextRole.BODY, tip);
        body.row((y, v) -> List.of(Dropdown.<E>builder().at(0, y).size(110, 16)
                .items(c.values())
                .label(e -> Labels.value(c, e))
                .state(c::get, c::set)
                .build()), Body.CONTROL_ROW);
    }
}

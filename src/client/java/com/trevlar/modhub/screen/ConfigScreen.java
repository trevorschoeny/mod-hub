package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.Flow;
import com.trevlar.menukit.api.element.PanelElement;
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
import com.trevlar.modhub.catalog.HubEntry;
import com.trevlar.modhub.catalog.ModCatalog;
import com.trevlar.modhub.catalog.ModInfo;
import com.trevlar.modhub.catalog.ModMenuBridge;
import com.trevlar.modhub.page.ConfigPage;
import com.trevlar.modhub.page.Pages;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A mod's or suite's settings: Inventory Plus's settings menu made general. Sidebar tabs
 * on the left, one per mod with settings; the page on the right; Done at the bottom; Escape back to
 * wherever it was opened from (the mod list, the pause menu, a container).
 *
 * <p>Each page is drawn from the mod's {@link ConfigPage} (see {@link Pages}) under IP's frame: the
 * mod's name, its description, Reset to Defaults, the rule, then its rows ({@link PageView}). A mod
 * whose page Mod Hub can't draw gets one button that opens its own screen.
 *
 * <p>Nothing to save: every control writes its setting on change.
 */
public final class ConfigScreen extends MKScreen {

    /** The open tab per entry, for the session. */
    private static final Map<String, String> SELECTED = new HashMap<>();
    private static final Map<String, Double> SCROLL = new HashMap<>();

    /** How far the menu starts below the name bar, clear of the top corners' buttons. */
    private static final int TOP_ROOM = 9;

    private ConfigScreen(HubEntry entry, @Nullable Screen parent, ScreenKind kind) {
        super(Component.literal(entry.name()), kind,
                List.of(main(entry, kind), done(), back(), modHub(parent), Confirm.panel("modhub:config_confirm")));
        // Both looks name the screen in a bar at twice size across the top, so the vanilla title
        // band goes.
        hideTitle();
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
        List<ModInfo> configurable = entry.mods().stream().filter(ModInfo::configurable).toList();
        int top = NameBar.HEIGHT + TOP_ROOM;
        Tabs.Builder tabs = Tabs.builder()
                .mode(Tabs.Mode.SIDEBAR).align(Tabs.Align.LEFT)
                .state(() -> SELECTED.get(entry.id()), id -> SELECTED.put(entry.id(), id))
                .bodyScroll(id -> SCROLL.get(entry.id() + "/" + id), (id, v) -> SCROLL.put(entry.id() + "/" + id, v));
        if (configurable.size() == 1) {
            ModInfo mod = configurable.getFirst();
            ConfigPage page = Pages.of(mod);
            // One mod on its own has no mods to switch between: its page, without the tabs, unless
            // its library splits it into categories, which then become the tabs. The panel scrolls
            // when the page is taller than the screen.
            if (page == null || page.categories().size() < 2) {
                return Panel.builder("modhub:config")
                        .style(options ? PanelStyle.NONE : PanelStyle.RAISED)
                        .position(PanelPosition.main())
                        .elements(page(mod, top))   // replaces the list, so first
                        .add(NameBar.of(entry.name(), 0))
                        .build();
            }
            for (int i = 0; i < page.categories().size(); i++) {
                ConfigPage.Category c = page.categories().get(i);
                Component label = c.name() != null ? c.name() : Component.literal(mod.name());
                tabs.tab(Tabs.tab("category" + i).label(label).body(() -> page(mod, page, List.of(c), false, 0)));
            }
        } else {
            for (ModInfo mod : configurable) {
                tabs.tab(Tabs.tab(mod.id()).label(Component.literal(mod.name())).body(() -> page(mod, 0)));
            }
        }
        // The name bar across the top, between Back and Mod Hub; the tabs start under it.
        tabs.at(0, top);
        Panel.Builder panel = Panel.builder("modhub:config")
                .style(options ? PanelStyle.NONE : PanelStyle.RAISED)
                .position(PanelPosition.main())
                .add(NameBar.of(entry.name(), 0));
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
    // the page's rows (PageView).

    /** One mod's page, every category on it, its rows starting at {@code top}. */
    private static List<PanelElement> page(ModInfo mod, int top) {
        ConfigPage page = Pages.of(mod);
        return page == null ? openOwnScreen(mod, top) : page(mod, page, page.categories(), true, top);
    }

    /** {@code mod}'s page showing {@code categories}; {@code headed} names each one above its rows. */
    private static List<PanelElement> page(ModInfo mod, ConfigPage page, List<ConfigPage.Category> categories,
                                           boolean headed, int top) {
        Body body = frame(mod, top);
        if (page.resetAll() != null) {
            body.row((y, v) -> List.of(Flow.builder().at(0, y).gap(10, 4)
                    .add(Button.builder().size(0, 16).label(Component.literal("Reset to Defaults"))
                            .onClick(() -> Confirm.ask("Reset " + mod.name() + " to defaults?",
                                    "Every setting on this tab goes back to how a fresh install has it.",
                                    page.resetAll()))
                            .build())
                    .build()), Body.CONTROL_ROW);
        }
        body.rule();
        PageView.draw(body, categories, headed);
        return body.build();
    }

    /** A mod whose settings Mod Hub can open but not draw: one button to its own screen. */
    private static List<PanelElement> openOwnScreen(ModInfo mod, int top) {
        Body body = frame(mod, top);
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

    /** The page's top: the mod's name in bold and its description. */
    private static Body frame(ModInfo mod, int top) {
        Body body = new Body().at(top);
        body.heading(mod.name());
        body.text(Component.literal(mod.description().isBlank() ? " " : mod.description()), TextRole.BODY);
        body.gap(4);
        return body;
    }
}

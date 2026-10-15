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

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
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

    private static final Identifier UNDO_ICON = Identifier.fromNamespaceAndPath("modhub", "icon/undo");
    private static final Identifier REDO_ICON = Identifier.fromNamespaceAndPath("modhub", "icon/redo");

    /** Undo and redo: the play session's history, shared by every config screen. */
    private final History history;

    private ConfigScreen(HubEntry entry, @Nullable Screen parent, ScreenKind kind, History history) {
        super(Component.literal(entry.name()), kind,
                List.of(main(entry, kind, history), done(history), back(), modHub(parent), Confirm.panel("modhub:config_confirm")));
        this.history = history;
        // Both looks name the screen in a bar at twice size across the top, so the vanilla title
        // band goes.
        hideTitle();
        setReturnAction(() -> Minecraft.getInstance().gui.setScreen(parent));
    }

    /**
     * Opens {@code entry}'s settings over {@code parent}. A mod in a suite opens the suite's screen
     * on that mod's tab. A lone mod Mod Hub can't draw (see Pages) opens its own screen directly, since a
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
        Pages.forget();
        List<ModInfo> mods = entry.mods().stream().filter(ModInfo::configurable).toList();
        // A lone mod Mod Hub can't draw opens its own screen straight away, rather than a page with
        // one button on it.
        if (mods.size() == 1 && Pages.of(mods.getFirst()) == null) {
            Screen own = ModMenuBridge.screen(mods.getFirst().id(), parent);
            if (own != null) {
                mc.gui.setScreen(own);
                return;
            }
        }
        ScreenKind kind = ModHubSettings.OPTIONS_LOOK.get() ? ScreenKind.OPTIONS : ScreenKind.CONTAINER;
        mc.gui.setScreen(new ConfigScreen(entry, parent, kind, History.SESSION));
    }

    private static Panel main(HubEntry entry, ScreenKind kind, History history) {
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
                        .elements(page(mod, top, history))   // replaces the list, so first
                        .add(NameBar.of(entry.name(), 0))
                        .build();
            }
            for (int i = 0; i < page.categories().size(); i++) {
                ConfigPage.Category c = page.categories().get(i);
                Component label = c.name() != null ? c.name() : Component.literal(mod.name());
                tabs.tab(Tabs.tab("category" + i).label(label).body(() -> page(mod, page, List.of(c), false, 0, history)));
            }
        } else {
            for (ModInfo mod : configurable) {
                tabs.tab(Tabs.tab(mod.id()).label(Component.literal(mod.name())).body(() -> page(mod, 0, history)));
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

    /**
     * Done at the bottom centre, as every Options screen ends, with Undo and Redo to its left,
     * greyed out while there's nothing to undo or redo. Each one's tooltip names the step it would
     * take. The panel is as wide again on Done's right, so Done stays centred. Done and Escape agree.
     */
    private static Panel done(History history) {
        Panel panel = Panel.builder("modhub:config_done")
                .style(PanelStyle.NONE)
                .position(PanelPosition.region(OutsideRegion.BOTTOM_CENTER))
                .add(Button.builder().at(0, 0).icon(UNDO_ICON).size(20, 20)
                        .tooltip(() -> stepTip("Undo", history.undoLabel()))
                        .disabledWhen(() -> !history.canUndo()).onClick(history::undo).build())
                .add(Button.builder().at(24, 0).icon(REDO_ICON).size(20, 20)
                        .tooltip(() -> stepTip("Redo", history.redoLabel()))
                        .disabledWhen(() -> !history.canRedo()).onClick(history::redo).build())
                .add(Button.builder().at(48, 0).label(CommonComponents.GUI_DONE).size(200, 20)
                        .onClick(() -> Minecraft.getInstance().gui.screen().onClose()).build())
                .build();
        return panel.pinnedWidth(48 + 200 + 48);
    }

    /** "Undo Particles", "Redo Reset to Defaults", or "Nothing to undo". */
    private static Component stepTip(String verb, @Nullable Component step) {
        return step == null ? Component.literal("Nothing to " + verb.toLowerCase(java.util.Locale.ROOT))
                : Component.literal(verb + " ").append(step);
    }

    /** Called when the world closes: the session's undo history ends with it. */
    public static void forgetHistory() {
        History.SESSION.clear();
    }

    /**
     * Ctrl+Z undoes and Ctrl+Shift+Z redoes (Cmd on a Mac), built in. Not while a text field has
     * focus: there the keys are the field's own.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_Z && event.hasControlDownWithQuirk() && !(getFocused() instanceof EditBox)) {
            if (event.hasShiftDown()) history.redo();
            else history.undo();
            return true;
        }
        return super.keyPressed(event);
    }

    // One mod's page
    //
    // Inventory Plus's frame and spacing (SettingsBody), so a Mod Hub page and an IP tab read the
    // same: the title in bold, the description, Reset to Defaults on its own row, the rule, then
    // the page's rows (PageView).

    /** One mod's page, every category on it, its rows starting at {@code top}. */
    private static List<PanelElement> page(ModInfo mod, int top, History history) {
        ConfigPage page = Pages.of(mod);
        return page == null ? openOwnScreen(mod, top) : page(mod, page, page.categories(), true, top, history);
    }

    /** {@code mod}'s page showing {@code categories}; {@code headed} names each one above its rows. */
    private static List<PanelElement> page(ModInfo mod, ConfigPage page, List<ConfigPage.Category> categories,
                                           boolean headed, int top, History history) {
        Body body = frame(mod, top);
        if (page.resetAll() != null) {
            body.row((y, v) -> List.of(Flow.builder().at(0, y).gap(10, 4)
                    .add(Button.builder().size(0, 16).label(Component.literal("Reset to Defaults"))
                            .onClick(() -> Confirm.ask("Reset " + mod.name() + " to defaults?",
                                    "Every setting on this tab goes back to how a fresh install has it.",
                                    // One step for the history, however many settings it changed.
                                    () -> history.asOneStep(History.values(page.rows().toList()),
                                            Component.literal("Reset to Defaults"), page.resetAll())))
                            .build())
                    .build()), Body.CONTROL_ROW);
        }
        body.rule();
        PageView.draw(body, categories, headed, history);
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

package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.Checkbox;
import com.trevlar.menukit.api.element.Dropdown;
import com.trevlar.menukit.api.element.Flow;
import com.trevlar.menukit.api.element.PanelElement;
import com.trevlar.menukit.api.element.Tabs;
import com.trevlar.menukit.api.element.TextField;
import com.trevlar.menukit.api.element.TextLabel;
import com.trevlar.menukit.api.element.TextRole;
import com.trevlar.menukit.api.panel.MKScreen;
import com.trevlar.menukit.api.panel.OutsideRegion;
import com.trevlar.menukit.api.panel.Panel;
import com.trevlar.menukit.api.panel.PanelPosition;
import com.trevlar.menukit.api.panel.PanelStyle;
import com.trevlar.menukit.api.panel.ScreenKind;
import com.trevlar.modhub.ModHubSettings;
import com.trevlar.modhub.catalog.HubEntry;
import com.trevlar.modhub.catalog.HubState;
import com.trevlar.modhub.catalog.ModCatalog;
import com.trevlar.modhub.catalog.ModInfo;
import com.trevlar.modhub.element.KeyButton;
import com.trevlar.modhub.element.ModCard;
import com.trevlar.modhub.element.ModIconImage;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The mod list: every installed mod, for finding it and for everything about it as a list entry.
 * Opened from the Mods button in the pause menu's icon row.
 *
 * <p>The sidebar is MenuKit's {@code Tabs}, one tab per entry ({@link ModCatalog#entries()}), with
 * a search field and New folder above it. Every page has one shape: a compact header, then the
 * controls row (configs, key, the shortcut's two checkboxes, folder). A suite, folder or
 * Auxiliary then lists each of its mods as a card with a controls row of its own.
 *
 * <p>The list's structure (which tabs exist, their names) is fixed when the screen is built, as
 * every MenuKit screen's is. A change to it (a folder made, renamed, deleted, or an entry moved)
 * builds the screen again on the same tab ({@link #rebuild}).
 */
public final class ModListScreen extends MKScreen {

    /** The Mods button's sprite: the pause menu's button and the config screen's are the same. */
    public static final net.minecraft.resources.Identifier MODS_SPRITE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("modhub", "pause_menu/mods");

    /** The open tab, the search text, and each tab's scroll, for the session. */
    private static String selected = "mod:modhub";
    private static String search = "";
    private static final Map<String, Double> SCROLL = new HashMap<>();

    /** Folder names being typed, by folder id, until Rename. */
    private static final Map<String, String> RENAMING = new HashMap<>();

    private final @Nullable Screen parent;

    private ModListScreen(@Nullable Screen parent, ScreenKind kind) {
        super(Component.literal("Mods"), kind, List.of(main(kind), done(), Confirm.panel("modhub:list_confirm")));
        this.parent = parent;
        if (kind == ScreenKind.CONTAINER) hideTitle();
        setReturnAction(() -> Minecraft.getInstance().gui.setScreen(parent));
    }

    public static void open(@Nullable Screen parent) {
        Minecraft.getInstance().gui.setScreen(create(parent));
    }

    private static ModListScreen create(@Nullable Screen parent) {
        return new ModListScreen(parent, ModHubSettings.OPTIONS_LOOK.get() ? ScreenKind.OPTIONS : ScreenKind.CONTAINER);
    }

    /** Builds the screen again over the same parent, on the next tick so no click loses its screen. */
    private static void rebuild() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.gui.screen() instanceof ModListScreen s) mc.gui.setScreen(create(s.parent));
        });
    }

    /** A key setting waiting for its key takes the press before anything else. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (KeyButton.keyPressed(event.input())) return true;
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        KeyButton.cancel();
        super.removed();
    }

    // The frame

    private static Panel main(ScreenKind kind) {
        Tabs.Builder tabs = Tabs.builder()
                .mode(Tabs.Mode.SIDEBAR).align(Tabs.Align.LEFT)
                .state(() -> selected, id -> selected = id)
                .bodyScroll(SCROLL::get, SCROLL::put)
                .sidebarHeader(
                        TextField.builder().size(100, 16).state(() -> search, s -> search = s).build(),
                        Button.builder().at(0, 20).size(100, 16).label(Component.literal("New folder"))
                                .onClick(() -> {
                                    selected = HubState.createFolder();
                                    rebuild();
                                }).build());
        for (HubEntry e : ModCatalog.entries()) {
            tabs.tab(Tabs.tab(e.id()).label(Component.literal(e.name()))
                    .visibleWhen(() -> matches(e))
                    .body(() -> page(e)));
        }
        boolean options = kind == ScreenKind.OPTIONS;
        if (!options) tabs.at(0, NameBar.HEIGHT + NameBar.GAP);
        Panel.Builder panel = Panel.builder("modhub:list")
                .style(options ? PanelStyle.NONE : PanelStyle.RAISED)
                .position(PanelPosition.main());
        if (!options) panel.add(NameBar.of("Mods"));
        return panel.add(tabs.build()).build();
    }

    private static Panel done() {
        return Panel.builder("modhub:list_done")
                .style(PanelStyle.NONE)
                .position(PanelPosition.region(OutsideRegion.BOTTOM_CENTER))
                .add(Button.builder().label(CommonComponents.GUI_DONE).size(200, 20)
                        .onClick(() -> Minecraft.getInstance().gui.screen().onClose()).build())
                .build();
    }

    /** Search: the entry's name, or any of its mods' names or ids. */
    private static boolean matches(HubEntry e) {
        String q = search.strip().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return true;
        if (e.name().toLowerCase(Locale.ROOT).contains(q)) return true;
        for (ModInfo m : e.mods()) {
            if (m.name().toLowerCase(Locale.ROOT).contains(q) || m.id().contains(q)) return true;
        }
        return false;
    }

    // Pages
    //
    // Every page starts the same: a compact header (icon, name, a line under it, the description)
    // and the entry's controls row. A suite, folder or Auxiliary then shows each of its mods as a
    // card, each with the same controls row of its own, so nothing has to be opened to be seen.

    /** The configs button's face, a gear. */
    private static final net.minecraft.resources.Identifier CONFIGS_ICON =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("modhub", "icon/configs");

    private static List<PanelElement> page(HubEntry e) {
        Body body = new Body();
        switch (e) {
            case HubEntry.Single s -> {
                header(body, s.name(), s.mod(), byline(s.mod()), s.mod().description());
                controls(body, s, true);
                links(body, s.mod());
            }
            case HubEntry.Suite s -> {
                header(body, s.name(), s.iconMod(), count(s.mods().size()) + " in this suite", null);
                controls(body, s, true);
                for (ModInfo m : s.mods()) card(body, new HubEntry.Single(m), m.description());
            }
            case HubEntry.Folder f -> {
                header(body, f.name(), f.iconMod(), "Your folder, " + count(f.mods().size()), null);
                folderControls(body, f);
                if (f.configurable()) controls(body, f, false);
                if (f.members().isEmpty()) {
                    body.text(Component.literal("Empty. Put a mod or suite in it from its own page (Folder)."),
                            TextRole.MUTED);
                }
                for (HubEntry m : f.members()) {
                    card(body, m, m instanceof HubEntry.Single one ? one.mod().description()
                            : count(m.mods().size()) + " in this suite");
                }
            }
            case HubEntry.Auxiliary a -> {
                header(body, a.name(), null, count(a.mods().size()), "Mods with no settings of their own: libraries, "
                        + "Fabric API's modules, and mods with nothing to set.");
                for (ModInfo m : a.mods()) card(body, new HubEntry.Single(m), m.description());
            }
        }
        return body.build();
    }

    private static String count(int n) {
        return n + (n == 1 ? " mod" : " mods");
    }

    private static String byline(ModInfo mod) {
        String v = mod.version();
        return mod.authors().isEmpty() ? v : v + "  ·  by " + String.join(", ", mod.authors());
    }

    /**
     * A page's top: the icon (or none, for Auxiliary), the name in bold, a muted line beside it, and
     * the description under them when there is one.
     */
    private static void header(Body body, String title, @Nullable ModInfo icon, String line, @Nullable String description) {
        body.row((y, v) -> {
            List<PanelElement> out = new ArrayList<>();
            int x = 0;
            if (icon != null) {
                out.add(new ModIconImage(0, y, 20, icon.id(), icon.name(), v));
                x = 26;
            }
            out.add(TextLabel.builder().at(x, y).text(Component.literal(title).withStyle(ChatFormatting.BOLD))
                    .role(TextRole.HEADING).visibleWhen(v).build());
            out.add(TextLabel.builder().at(x, y + 11).text(Component.literal(line))
                    .role(TextRole.MUTED).visibleWhen(v).build());
            return out;
        }, 24);
        if (description != null && !description.isBlank()) body.text(Component.literal(description), TextRole.BODY);
        body.gap(4);
    }

    /**
     * The controls row, the same for every entry: the configs button (a gear), the key, then the
     * shortcut's two checkboxes, then (for a mod or suite at the top level) its folder. Wraps when
     * the page is narrow. An entry with no settings has nothing for a shortcut to open, so its
     * row is only the folder.
     */
    private static void controls(Body body, HubEntry e, boolean folder) {
        body.row((y, v) -> List.of(controlsRow(0, y, e, folder, v)), Body.CONTROL_ROW + 2);
        body.gap(Body.GAP);
    }

    private static PanelElement controlsRow(int x, int y, HubEntry e, boolean folder, java.util.function.BooleanSupplier v) {
        Flow.Builder row = Flow.builder().at(x, y).gap(6, 4).visibleWhen(v);
        if (e.configurable()) {
            row.add(Button.builder().icon(CONFIGS_ICON).size(16, 16)
                    .tooltip(Component.literal(e.name() + " configs"))
                    .onClick(() -> ConfigScreen.open(e, Minecraft.getInstance().gui.screen())).build());
            row.add(KeyButton.of(0, 0, e.id()));
            row.add(Checkbox.builder().label(Component.literal("Pause menu"))
                    .tooltip(Component.literal("A shortcut to " + e.name() + " at the top of the pause menu"))
                    .state(() -> HubState.pause(e.id()), on -> HubState.setPause(e.id(), on)).build());
            row.add(Checkbox.builder().label(Component.literal("Inventory"))
                    .tooltip(Component.literal("A shortcut to " + e.name() + " at the top of inventories and containers"))
                    .state(() -> HubState.inventory(e.id()), on -> HubState.setInventory(e.id(), on)).build());
        }
        if (folder) row.add(folderChoice(e));
        return row.build();
    }

    /** Which folder the entry is in, or None. Moving it changes the sidebar, so it rebuilds. */
    private static PanelElement folderChoice(HubEntry e) {
        List<String> choices = new ArrayList<>();
        choices.add("");   // the top level
        Map<String, String> names = new HashMap<>();
        for (HubState.FolderData f : HubState.folders()) {
            choices.add(f.id);
            names.put(f.id, f.name);
        }
        return Dropdown.<String>builder().size(110, 16).items(choices)
                .label(id -> Component.literal("Folder: " + (id.isEmpty() ? "None" : names.getOrDefault(id, id))))
                .state(() -> Objects.requireNonNullElse(HubState.folderOf(e.id()), ""),
                        id -> {
                            String now = Objects.requireNonNullElse(HubState.folderOf(e.id()), "");
                            if (id.equals(now)) return;
                            HubState.setFolder(e.id(), id.isEmpty() ? null : id);
                            rebuild();
                        })
                .disabledWhen(() -> choices.size() == 1)
                .tooltip(Component.literal(choices.size() == 1 ? "Make a folder first (New folder)."
                        : "Put this in one of your folders."))
                .build();
    }

    /** A folder's name, with Rename, and Delete. */
    private static void folderControls(Body body, HubEntry.Folder f) {
        body.row((y, v) -> List.of(
                TextField.builder().at(0, y).size(120, 16)
                        .state(() -> RENAMING.getOrDefault(f.id(), f.name()), s -> RENAMING.put(f.id(), s))
                        .visibleWhen(v).build(),
                Button.builder().at(124, y).size(0, 16).label(Component.literal("Rename")).visibleWhen(v)
                        .onClick(() -> {
                            String name = RENAMING.remove(f.id());
                            if (name != null) HubState.renameFolder(f.id(), name);
                            rebuild();
                        }).build(),
                Button.builder().at(184, y).size(0, 16).label(Component.literal("Delete folder")).visibleWhen(v)
                        .onClick(() -> Confirm.ask("Delete " + f.name() + "?",
                                "Its mods go back to the list. Their settings don't change.", () -> {
                                    HubState.deleteFolder(f.id());
                                    selected = "mod:modhub";
                                    rebuild();
                                })).build()), Body.CONTROL_ROW);
        body.gap(Body.GAP);
    }

    /**
     * One member as a card: its icon, name, version and a line of description, and its own
     * controls row inside the card when it has settings. A mod in a suite has its own shortcut and
     * key, apart from the suite's.
     */
    private static void card(Body body, HubEntry member, String description) {
        ModInfo icon = member.iconMod();
        boolean controls = member.configurable();
        String version = member instanceof HubEntry.Single s ? s.mod().version() : "";
        body.row((y, v) -> {
            List<PanelElement> out = new ArrayList<>();
            out.add(new ModCard(y, controls, icon == null ? "" : icon.id(), member.name(), version, description, v));
            if (controls) out.add(controlsRow(ModCard.TEXT_X, y + ModCard.CONTROLS_Y - 4, member, false, v));
            return out;
        }, (controls ? ModCard.HEIGHT_WITH_CONTROLS : ModCard.HEIGHT_PLAIN) + 4);
    }

    /** The mod's links, each through vanilla's "open this link?" screen. */
    private static void links(Body body, ModInfo mod) {
        if (mod.links().isEmpty()) return;
        body.row((y, v) -> {
            Flow.Builder row = Flow.builder().at(0, y).gap(4, 4).visibleWhen(v);
            mod.links().forEach((label, url) -> row.add(Button.builder().size(0, 16).label(Component.literal(label))
                    .tooltip(Component.literal(url))
                    .onClick(() -> {
                        try {
                            ConfirmLinkScreen.confirmLinkNow(Minecraft.getInstance().gui.screen(), URI.create(url));
                        } catch (IllegalArgumentException bad) {
                            // a malformed URL in someone's fabric.mod.json: the button does nothing
                        }
                    }).build()));
            return List.of(row.build());
        }, Body.CONTROL_ROW);
    }
}

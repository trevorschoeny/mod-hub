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
 * a search field and New folder above it. A tab's page is the entry's details: its icon, name,
 * version, authors and description, Configure, then the list settings (shortcut, key, folder),
 * then its links. A suite, folder or Auxiliary lists its mods instead; clicking one shows that
 * mod's details in place, with Back.
 *
 * <p>The list's structure (which tabs exist, their names) is fixed when the screen is built, as
 * every MenuKit screen's is. A change to it (a folder made, renamed, deleted, or an entry moved)
 * builds the screen again on the same tab ({@link #rebuild}).
 */
public final class ModListScreen extends MKScreen {

    /** The open tab, the search text, and each tab's scroll, for the session. */
    private static String selected = "mod:modhub";
    private static String search = "";
    private static final Map<String, Double> SCROLL = new HashMap<>();

    /** In a suite, folder or Auxiliary: the member whose details are showing, by entry id. */
    private static final Map<String, String> MEMBER = new HashMap<>();

    /** Folder names being typed, by folder id, until Rename. */
    private static final Map<String, String> RENAMING = new HashMap<>();

    /** The shortcut row's order list shows this many places at most. */
    private static final int ORDER_ROWS = 24;

    private final @Nullable Screen parent;

    private ModListScreen(@Nullable Screen parent, ScreenKind kind) {
        super(Component.literal("Mods"), kind, List.of(main(kind), done(), Confirm.panel("modhub:list_confirm")));
        this.parent = parent;
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
        return Panel.builder("modhub:list")
                .style(kind == ScreenKind.OPTIONS ? PanelStyle.NONE : PanelStyle.RAISED)
                .position(PanelPosition.main())
                .add(tabs.build())
                .build();
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

    private static List<PanelElement> page(HubEntry e) {
        Body body = new Body();
        switch (e) {
            case HubEntry.Single s -> {
                modDetails(body, s.mod(), true);
                body.gap(Body.GAP);
                listSettings(body, s);
                if (s.mod().id().equals("modhub")) shortcutOrder(body);
                links(body, s.mod());
            }
            case HubEntry.Suite s -> {
                groupHeader(body, s, s.mods().size() + (s.mods().size() == 1 ? " mod" : " mods") + " in this suite");
                listSettings(body, s);
                members(body, s, "Mods in this suite");
            }
            case HubEntry.Folder f -> {
                groupHeader(body, f, "Your folder");
                folderControls(body, f);
                if (f.configurable()) listSettings(body, f);
                if (f.members().isEmpty()) {
                    body.text(Component.literal("Empty. Add a mod or suite from its own page (Folder)."), TextRole.MUTED);
                } else {
                    members(body, f, "In this folder");
                }
            }
            case HubEntry.Auxiliary a -> {
                body.heading("Auxiliary");
                body.text(Component.literal("Mods with no settings of their own: libraries, Fabric API's"), TextRole.MUTED);
                body.text(Component.literal("modules, and mods with nothing to set."), TextRole.MUTED);
                body.gap(Body.GAP);
                members(body, a, a.mods().size() + " mods");
            }
        }
        return body.build();
    }

    /**
     * A mod's icon, name, version, authors and description. With {@code preview}, also the icon at
     * the sizes a tiny sidebar icon could be, for Trev to pick from (design, Decided 2).
     */
    private static void modDetails(Body body, ModInfo mod, boolean preview) {
        body.row((y, v) -> List.of(
                new ModIconImage(0, y, 32, mod.id(), mod.name(), v),
                TextLabel.builder().at(40, y + 2).text(Component.literal(mod.name()).withStyle(ChatFormatting.BOLD))
                        .role(TextRole.HEADING).visibleWhen(v).build(),
                TextLabel.builder().at(40, y + 16).text(Component.literal(byline(mod)))
                        .role(TextRole.MUTED).visibleWhen(v).build()), 38);
        if (!mod.description().isBlank()) body.text(Component.literal(mod.description()), TextRole.BODY);
        if (preview) {
            body.gap(4);
            body.text(Component.literal("Tiny icon preview, as the sidebar could show it:"), TextRole.MUTED);
            body.row((y, v) -> List.of(
                    new ModIconImage(0, y + 1, 8, mod.id(), mod.name(), v),
                    TextLabel.builder().at(11, y + 1).text(Component.literal(mod.name() + "  (8 px)"))
                            .visibleWhen(v).build(),
                    new ModIconImage(110, y, 10, mod.id(), mod.name(), v),
                    TextLabel.builder().at(123, y + 1).text(Component.literal(mod.name() + "  (10 px)"))
                            .visibleWhen(v).build()), Body.TEXT_ROW + 2);
        }
    }

    private static String byline(ModInfo mod) {
        String v = mod.version();
        return mod.authors().isEmpty() ? v : v + "  ·  by " + String.join(", ", mod.authors());
    }

    /** A suite's or folder's top: its icon (its first mod's), name and a line under it. */
    private static void groupHeader(Body body, HubEntry e, String line) {
        ModInfo icon = e.iconMod();
        body.row((y, v) -> {
            List<PanelElement> out = new ArrayList<>();
            if (icon != null) out.add(new ModIconImage(0, y, 32, icon.id(), icon.name(), v));
            int x = icon != null ? 40 : 0;
            out.add(TextLabel.builder().at(x, y + 2).text(Component.literal(e.name()).withStyle(ChatFormatting.BOLD))
                    .role(TextRole.HEADING).visibleWhen(v).build());
            out.add(TextLabel.builder().at(x, y + 16).text(Component.literal(line))
                    .role(TextRole.MUTED).visibleWhen(v).build());
            return out;
        }, 38);
        body.gap(Body.GAP);
    }

    /**
     * Configure, then the entry's list settings: its shortcut (pause menu, inventory), its key,
     * and (for a mod or suite) its folder. Nothing for an entry with no settings: there's nothing
     * for a shortcut to open.
     */
    private static void listSettings(Body body, HubEntry e) {
        if (!e.configurable()) {
            body.text(Component.literal("No settings."), TextRole.MUTED);
            body.gap(Body.GAP);
            if (!(e instanceof HubEntry.Folder)) folderChoice(body, e);
            return;
        }
        body.row((y, v) -> List.of(Button.builder().at(0, y).size(110, 20)
                .label(Component.literal("Configure")).visibleWhen(v)
                .onClick(() -> ConfigScreen.open(e, Minecraft.getInstance().gui.screen())).build()), 26);
        body.row((y, v) -> List.of(Flow.builder().at(0, y).gap(10, 4).visibleWhen(v)
                .add(TextLabel.builder().text(Component.literal("Shortcut")).build())
                .add(Checkbox.builder().label(Component.literal("Pause menu"))
                        .state(() -> HubState.pause(e.id()), on -> HubState.setPause(e.id(), on)).build())
                .add(Checkbox.builder().label(Component.literal("Inventory"))
                        .state(() -> HubState.inventory(e.id()), on -> HubState.setInventory(e.id(), on)).build())
                .build()), Body.TEXT_ROW + 8);
        body.row((y, v) -> List.of(
                TextLabel.builder().at(0, y + 4).text(Component.literal("Key")).visibleWhen(v).build(),
                KeyButton.of(54, y, e.id())), Body.CONTROL_ROW);
        if (!(e instanceof HubEntry.Folder)) folderChoice(body, e);
        body.gap(Body.GAP);
    }

    /** Which folder the entry is in, or None. Moving it changes the sidebar, so it rebuilds. */
    private static void folderChoice(Body body, HubEntry e) {
        List<String> choices = new ArrayList<>();
        choices.add("");   // the top level
        Map<String, String> names = new HashMap<>();
        for (HubState.FolderData f : HubState.folders()) {
            choices.add(f.id);
            names.put(f.id, f.name);
        }
        body.row((y, v) -> List.of(
                TextLabel.builder().at(0, y + 4).text(Component.literal("Folder")).visibleWhen(v).build(),
                Dropdown.<String>builder().at(54, y).size(110, 16).items(choices)
                        .label(id -> Component.literal(id.isEmpty() ? "None" : names.getOrDefault(id, id)))
                        .state(() -> Objects.requireNonNullElse(HubState.folderOf(e.id()), ""),
                                id -> {
                                    String now = Objects.requireNonNullElse(HubState.folderOf(e.id()), "");
                                    if (id.equals(now)) return;
                                    HubState.setFolder(e.id(), id.isEmpty() ? null : id);
                                    rebuild();
                                })
                        .disabledWhen(() -> choices.size() == 1)
                        .tooltip(Component.literal(choices.size() == 1 ? "Make a folder first (New folder)." : "Put this in one of your folders."))
                        .visibleWhen(v).build()), Body.CONTROL_ROW);
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
     * A group's mods: one row each (tiny icon, name, version), and in the same place, each one's
     * details with Back, shown while it's picked. A folder's members are whole entries, so a suite
     * in a folder shows as its mods.
     */
    private static void members(Body body, HubEntry group, String title) {
        String key = group.id();
        body.when(() -> !MEMBER.containsKey(key));
        int top = body.y();
        body.heading(title);
        for (ModInfo m : group.mods()) {
            body.row((y, v) -> List.of(
                    new ModIconImage(0, y + 2, 12, m.id(), m.name(), v),
                    Button.builder().at(16, y).size(220, 16).visibleWhen(v)
                            .label(Component.literal(m.name() + "   " + m.version()))
                            .onClick(() -> MEMBER.put(key, m.id())).build()), 18);
        }
        // Each member's details, laid over the list, shown only while picked.
        for (ModInfo m : group.mods()) {
            body.at(top).when(() -> m.id().equals(MEMBER.get(key)));
            body.row((y, v) -> List.of(Button.builder().at(0, y).size(60, 16).label(Component.literal("< Back"))
                    .visibleWhen(v).onClick(() -> MEMBER.remove(key)).build()), Body.CONTROL_ROW);
            modDetails(body, m, false);
            body.gap(Body.GAP);
            if (m.configurable()) {
                body.row((y, v) -> List.of(Button.builder().at(0, y).size(110, 20)
                        .label(Component.literal("Configure")).visibleWhen(v)
                        .onClick(() -> ConfigScreen.open(new HubEntry.Single(m), Minecraft.getInstance().gui.screen()))
                        .build()), 26);
            }
            links(body, m);
        }
        body.when(() -> true);
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

    /**
     * Mod Hub's own page: the shortcut row's order, one line per shortcut with Left and Right.
     * ponytail: a fixed {@link #ORDER_ROWS} lines, each showing whichever shortcut is in its place
     * now, since the screen's structure is fixed at build. Raise it if anyone pins more.
     */
    private static void shortcutOrder(Body body) {
        body.heading("Shortcut order");
        body.when(() -> HubState.shortcutOrder().isEmpty());
        body.text(Component.literal("No shortcuts yet. Turn one on from a mod's page."), TextRole.MUTED);
        for (int i = 0; i < ORDER_ROWS; i++) {
            int place = i;
            body.when(() -> place < HubState.shortcutOrder().size());
            body.row((y, v) -> List.of(
                    Button.builder().at(0, y).size(16, 16).label(Component.literal("<")).visibleWhen(v)
                            .disabledWhen(() -> place == 0)
                            .onClick(() -> HubState.move(HubState.shortcutOrder().get(place), -1)).build(),
                    Button.builder().at(18, y).size(16, 16).label(Component.literal(">")).visibleWhen(v)
                            .disabledWhen(() -> place >= HubState.shortcutOrder().size() - 1)
                            .onClick(() -> HubState.move(HubState.shortcutOrder().get(place), 1)).build(),
                    TextLabel.builder().at(40, y + 4).visibleWhen(v)
                            .text(() -> Component.literal(orderLabel(place))).build()), 18);
        }
        body.when(() -> true);
        body.gap(Body.GAP);
    }

    private static String orderLabel(int place) {
        List<String> order = HubState.shortcutOrder();
        if (place >= order.size()) return "";
        String id = order.get(place);
        HubEntry e = ModCatalog.find(id);
        String where = (HubState.pause(id) ? "pause menu" : "") + (HubState.pause(id) && HubState.inventory(id) ? ", " : "")
                + (HubState.inventory(id) ? "inventory" : "");
        return (place + 1) + ". " + (e != null ? e.name() : id + " (not installed)") + "  (" + where + ")";
    }
}

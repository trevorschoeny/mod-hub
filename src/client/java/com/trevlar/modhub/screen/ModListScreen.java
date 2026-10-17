package com.trevlar.modhub.screen;

import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.Checkbox;
import com.trevlar.menukit.api.element.Flow;
import com.trevlar.menukit.api.element.PanelElement;
import com.trevlar.menukit.api.element.Section;
import com.trevlar.menukit.api.element.Tabs;
import com.trevlar.menukit.api.element.TextField;
import com.trevlar.menukit.api.element.TextLabel;
import com.trevlar.menukit.api.element.TextRole;
import com.trevlar.menukit.api.layout.CrossAlign;
import com.trevlar.menukit.api.layout.Row;
import com.trevlar.menukit.api.panel.MKScreen;
import com.trevlar.menukit.api.panel.OutsideRegion;
import com.trevlar.menukit.api.panel.Panel;
import com.trevlar.menukit.api.panel.PanelPosition;
import com.trevlar.menukit.api.panel.PanelStyle;
import com.trevlar.menukit.api.panel.ScreenKind;
import com.trevlar.modhub.ModHubSettings;
import com.trevlar.modhub.catalog.HubEntry;
import com.trevlar.modhub.catalog.HubState;
import com.trevlar.modhub.catalog.KeybinderyBridge;
import com.trevlar.modhub.catalog.ModCatalog;
import com.trevlar.modhub.catalog.ModIcons;
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

/**
 * The mod list: every installed mod, for finding it and for everything about it as a list entry.
 * Opened from the Mods button in the pause menu's icon row.
 *
 * <p>The sidebar is MenuKit's {@code Tabs}, one tab per entry ({@link ModCatalog#entries()}), with
 * a search field above it. Every page has one shape: a compact header, then the controls row
 * (configs, Keybindery's keys when it's installed, the key, the shortcut's two checkboxes). A suite
 * or Auxiliary then lists each of its mods as a card with a row of its own. A mod's description
 * is a Description section that opens in place; a suite's sits under its name.
 */
public final class ModListScreen extends MKScreen {

    /** The Mods button's sprite: the pause menu's button and the config screen's are the same. */
    public static final net.minecraft.resources.Identifier MODS_SPRITE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("modhub", "pause_menu/mods");

    /** The open tab, the search text, and each tab's scroll, for the session. */
    private static String selected = "mod:modhub";
    private static String search = "";
    private static final Map<String, Double> SCROLL = new HashMap<>();

    /** Which mods' Description sections are open, by mod id, for the session. */
    private static final Map<String, Boolean> DESCRIPTION_OPEN = new HashMap<>();

    private final @Nullable Screen parent;

    private ModListScreen(@Nullable Screen parent, ScreenKind kind) {
        super(Component.literal("Mods"), kind, List.of(main(kind), done()));
        this.parent = parent;
        if (kind == ScreenKind.PANEL) hideTitle();
        setReturnAction(() -> Minecraft.getInstance().gui.setScreen(parent));
    }

    public static void open(@Nullable Screen parent) {
        Minecraft.getInstance().gui.setScreen(create(parent));
    }

    /** The mod list over {@code parent}. Public for the Mod Menu API stand-in's {@code createModsScreen}. */
    public static ModListScreen create(@Nullable Screen parent) {
        return new ModListScreen(parent, ModHubSettings.OPTIONS_LOOK.get() ? ScreenKind.DEFAULT : ScreenKind.PANEL);
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
                .state(() -> selected, ModListScreen::select)
                .bodyScroll(SCROLL::get, SCROLL::put)
                .sidebarHeader(TextField.builder().size(100, 16).hint(Component.literal("Search"))
                        .state(() -> search, ModListScreen::search).build());
        for (HubEntry e : ModCatalog.entries()) {
            Tabs.TabSpec tab = Tabs.tab(e.id()).label(Component.literal(e.name()))
                    .visibleWhen(() -> matches(e))
                    .body(() -> page(e));
            // The mod's own icon at 10 px beside its name. None for Auxiliary, or for a mod that
            // ships no icon.
            ModIcons.Icon icon = ModIcons.of(e.iconKey());
            if (icon != null) tab.icon(icon::texture, icon.size(), 10);
            tabs.tab(tab);
        }
        boolean options = kind == ScreenKind.DEFAULT;
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

    /** Each suite card's y on its page, by suite entry id then mod id, filled as pages are built. */
    private static final Map<String, Map<String, Integer>> CARD_Y = new HashMap<>();

    /**
     * Picks a tab. With a search typed, a suite opens scrolled to its first mod whose name
     * matches, since that's what the player searched for. The y is the card's with every
     * Description closed, so an open one above it lands the scroll a little short.
     */
    private static void select(String id) {
        selected = id;
        String q = search.strip().toLowerCase(Locale.ROOT);
        Map<String, Integer> cards = CARD_Y.get(id);
        if (q.isEmpty() || cards == null) return;
        HubEntry e = ModCatalog.find(id);
        if (e == null || e.name().toLowerCase(Locale.ROOT).contains(q)) return;   // the suite itself matched
        for (ModInfo m : e.mods()) {
            Integer y = cards.get(m.id());
            if (y != null && m.name().toLowerCase(Locale.ROOT).contains(q)) {
                SCROLL.put(id, (double) Math.max(0, y - 4));
                return;
            }
        }
    }

    /**
     * A new search: every page's scroll goes back to the top, so the next suite picked scrolls to
     * its match from a clean start rather than from where the last search left it.
     */
    private static void search(String query) {
        if (query.equals(search)) return;
        search = query;
        SCROLL.clear();
    }

    /** Search: the entry's name, or any of its mods' names. Names only, never descriptions. */
    private static boolean matches(HubEntry e) {
        String q = search.strip().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return true;
        if (e.name().toLowerCase(Locale.ROOT).contains(q)) return true;
        for (ModInfo m : e.mods()) {
            if (m.name().toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    // Pages
    //
    // Every page starts the same: a compact header and the entry's controls row. A suite or
    // Auxiliary then shows each of its mods as a card, each with the same controls row of its own,
    // so nothing has to be opened to be seen.

    /** The configs button's face, a gear. */
    private static final net.minecraft.resources.Identifier CONFIGS_ICON =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("modhub", "icon/configs");

    private static List<PanelElement> page(HubEntry e) {
        Body body = new Body();
        switch (e) {
            case HubEntry.Single s -> {
                // One mod on its own: the description in full, its links under it, no dropdown.
                header(body, s.name(), s.iconKey(), byline(s.mod()), s.mod().description());
                if (!s.mod().links().isEmpty()) {
                    body.row((y, v) -> links(0, y, s.mod(), v), Body.CONTROL_ROW);
                    body.gap(2);
                }
                controls(body, s);
            }
            case HubEntry.Suite s -> {
                header(body, s.name(), s.iconKey(), count(s.mods().size()) + " in this suite", null);
                controls(body, s);
                for (ModInfo m : s.mods()) card(body, s.id(), m);
            }
            case HubEntry.Auxiliary a -> {
                header(body, a.name(), null, count(a.mods().size()), "Mods with no settings of their own: libraries, "
                        + "Fabric API's modules, and mods with nothing to set.");
                for (ModInfo m : a.mods()) card(body, a.id(), m);
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
     * A page's top: the icon (or none, for Auxiliary), the name in bold, a muted line under it, and
     * a description under them when there is one (a group's own; a mod's is a section instead).
     */
    private static void header(Body body, String title, @Nullable String iconKey, String line, @Nullable String description) {
        body.row((y, v) -> {
            List<PanelElement> out = new ArrayList<>();
            int x = 0;
            // No icon: no picture, and the name and line start at the left edge.
            if (ModIcons.of(iconKey) != null) {
                out.add(new ModIconImage(0, y, 20, iconKey, v));
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

    /** Whether a mod has anything for its Description section: the text, or a link. */
    private static boolean hasDescription(ModInfo mod) {
        return !mod.description().isBlank() || !mod.links().isEmpty();
    }

    /**
     * A mod's description as a section in its card: closed, its header is the description's first
     * line ending in "..."; open, the whole description, then its links (Homepage, Source, Report
     * an issue). The section re-flows when the text wraps, so the links stay below it.
     */
    private static Section descriptionSection(int x, int y, ModInfo mod, java.util.function.BooleanSupplier v) {
        List<PanelElement> content = new ArrayList<>();
        int linksY = 0;
        if (!mod.description().isBlank()) {
            content.add(TextLabel.builder().at(0, 0).text(Component.literal(mod.description())).role(TextRole.BODY).build());
            linksY = Body.TEXT_ROW + 4;
        }
        if (!mod.links().isEmpty()) content.addAll(links(0, linksY, mod, () -> true));
        String first = firstLine(mod);
        // The header is the arrow and a summary (MenuKit cuts a summary to fit, with "...");
        // the summary goes blank while open, as the text is right under it.
        return Section.builder(Component.empty())
                .summary(() -> Component.literal(DESCRIPTION_OPEN.getOrDefault(mod.id(), false) ? "" : first))
                .at(x, y)
                .state(() -> DESCRIPTION_OPEN.getOrDefault(mod.id(), false), open -> DESCRIPTION_OPEN.put(mod.id(), open))
                .content(content)
                .visibleWhen(v)
                .build();
    }

    /** The controls under a page's header. */
    private static void controls(Body body, HubEntry e) {
        if (!e.configurable()) {
            body.text(Component.literal("No settings."), TextRole.MUTED);
            body.gap(Body.GAP);
            return;
        }
        body.row((y, v) -> controlsRows(0, y, e, v), ModCard.CONTROLS_HEIGHT + 2);
        body.gap(Body.GAP);
    }

    /** Icon buttons are 19 px: a 15 px sprite (the pause menu's size) drawn 1:1 inside MenuKit's 2 px inset. */
    private static final int ICON_BUTTON = 19;

    /**
     * The controls, the same for every entry, in one row centred on its height: the configs button
     * (a gear), Keybindery's keys button when Keybindery is installed, the key, then the shortcut's
     * two checkboxes. A {@code Row}, not a {@code Flow}, because only {@code Row} centres
     * children of different heights; it doesn't wrap, which the row's fixed widths never need.
     */
    private static List<PanelElement> controlsRows(int x, int y, HubEntry e, java.util.function.BooleanSupplier v) {
        String what = e instanceof HubEntry.Suite ? "the " + e.name() + " suite" : e.name();
        var row = Row.at(x, y).spacing(6).crossAlign(CrossAlign.CENTER);
        row.add(Button.builder().icon(CONFIGS_ICON).size(ICON_BUTTON, ICON_BUTTON).visibleWhen(v)
                .tooltip(Component.literal(e.name() + " configs"))
                .onClick(() -> ConfigScreen.open(e, Minecraft.getInstance().gui.screen())).build());
        if (KeybinderyBridge.present()) {
            List<String> ids = e.mods().stream().map(ModInfo::id).toList();
            row.add(Button.builder().icon(KeybinderyBridge.KEYS_SPRITE).size(ICON_BUTTON, ICON_BUTTON).visibleWhen(v)
                    .tooltip(Component.literal(e.name() + " keybinds, in Keybindery"))
                    .onClick(() -> KeybinderyBridge.open(ids, Minecraft.getInstance().gui.screen())).build());
        }
        row.add(KeyButton.of(0, 0, e.id(), v));
        row.add(Checkbox.builder().label(Component.literal("Pause")).visibleWhen(v)
                .tooltip(Component.literal("Adds a button to open the config for " + what + " at the top of the pause menu"))
                .state(() -> HubState.pause(e.id()), on -> HubState.setPause(e.id(), on)).build());
        row.add(Checkbox.builder().label(Component.literal("Inventory")).visibleWhen(v)
                .tooltip(Component.literal("Adds a button to open the config for " + what + " at the top of the inventory"))
                .state(() -> HubState.inventory(e.id()), on -> HubState.setInventory(e.id(), on)).build());
        return row.build();
    }

    /** A description's first line (to its first line break or sentence end), with "..." after it. */
    private static String firstLine(ModInfo mod) {
        String d = mod.description().strip();
        int cut = d.indexOf('\n');
        int stop = d.indexOf(". ");
        if (stop >= 0 && (cut < 0 || stop < cut)) cut = stop + 1;
        String line = cut < 0 ? d : d.substring(0, cut).strip();
        return line.isEmpty() ? "Links" : line + "...";
    }

    /**
     * One mod as a card: its icon, name and version, its Description section, and its own controls
     * row when it has settings, all inside the card's frame and across its whole width. The card
     * grows with the section; the panel's reflow moves the cards after it.
     */
    private static void card(Body body, String pageId, ModInfo m) {
        CARD_Y.computeIfAbsent(pageId, k -> new HashMap<>()).put(m.id(), body.y());
        HubEntry entry = new HubEntry.Single(m);
        boolean desc = hasDescription(m);
        boolean controls = m.configurable();
        body.row((y, v) -> {
            List<PanelElement> out = new ArrayList<>();
            Section section = desc ? descriptionSection(ModCard.PAD, y + ModCard.SECTION_Y, m, v) : null;
            out.add(new ModCard(y, m.id(), m.name(), m.version(), desc, controls,
                    () -> section == null ? 0 : section.extraLayoutHeight(), v));
            if (section != null) out.add(section);
            if (controls) out.addAll(controlsRows(ModCard.PAD, y + ModCard.controlsY(desc), entry, v));
            return out;
        }, ModCard.height(desc, controls) + 4);
    }

    /**
     * The mod's links as buttons in a row, each through vanilla's "open this link?" screen. Placed
     * one after another at their label's width, rather than in a {@code Flow}, so they lay out the
     * same inside a section's content as on a page.
     */
    private static List<PanelElement> links(int x, int y, ModInfo mod, java.util.function.BooleanSupplier v) {
        var font = Minecraft.getInstance().font;
        List<PanelElement> out = new ArrayList<>();
        int bx = x;
        for (var link : mod.links().entrySet()) {
            String label = link.getKey(), url = link.getValue();
            int w = font.width(label) + 12;
            out.add(Button.builder().at(bx, y).size(w, 16).label(Component.literal(label))
                    .tooltip(Component.literal(url)).visibleWhen(v)
                    .onClick(() -> {
                        try {
                            ConfirmLinkScreen.confirmLinkNow(Minecraft.getInstance().gui.screen(), URI.create(url));
                        } catch (IllegalArgumentException bad) {
                            // a malformed URL in someone's fabric.mod.json: the button does nothing
                        }
                    }).build());
            bx += w + 4;
        }
        return out;
    }
}

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
                .sidebarHeader(TextField.builder().size(100, 16).state(() -> search, s -> search = s).build());
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
                header(body, s.name(), s.mod(), byline(s.mod()), null);
                description(body, 0, s.mod());
                controls(body, s);
                links(body, s.mod());
            }
            case HubEntry.Suite s -> {
                header(body, s.name(), s.iconMod(), count(s.mods().size()) + " in this suite", null);
                controls(body, s);
                for (ModInfo m : s.mods()) card(body, m);
            }
            case HubEntry.Auxiliary a -> {
                header(body, a.name(), null, count(a.mods().size()), "Mods with no settings of their own: libraries, "
                        + "Fabric API's modules, and mods with nothing to set.");
                for (ModInfo m : a.mods()) card(body, m);
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

    /** A mod's description as a Description section, closed to start; nothing when it has none. */
    private static @Nullable Section description(Body body, int x, ModInfo mod) {
        if (mod.description().isBlank()) return null;
        Section[] made = new Section[1];
        body.row((y, v) -> {
            made[0] = descriptionSection(x, y, mod, v);
            return List.of(made[0]);
        }, Section.HEADER_HEIGHT + 4);
        return made[0];
    }

    private static Section descriptionSection(int x, int y, ModInfo mod, java.util.function.BooleanSupplier v) {
        return Section.builder(Component.literal("Description"))
                .at(x, y)
                .state(() -> DESCRIPTION_OPEN.getOrDefault(mod.id(), false), open -> DESCRIPTION_OPEN.put(mod.id(), open))
                .content(List.of(TextLabel.builder().text(Component.literal(mod.description()))
                        .role(TextRole.BODY).build()))
                .visibleWhen(v)
                .build();
    }

    /** The controls row under a page's header. */
    private static void controls(Body body, HubEntry e) {
        if (!e.configurable()) {
            body.text(Component.literal("No settings."), TextRole.MUTED);
            body.gap(Body.GAP);
            return;
        }
        body.row((y, v) -> List.of(controlsRow(0, y, e, v)), Body.CONTROL_ROW + 2);
        body.gap(Body.GAP);
    }

    /**
     * The controls row, the same for every entry: the configs button (a gear), Keybindery's keys
     * button when Keybindery is installed, the key, then the shortcut's two checkboxes. Wraps when
     * the page is narrow.
     */
    private static PanelElement controlsRow(int x, int y, HubEntry e, java.util.function.BooleanSupplier v) {
        Flow.Builder row = Flow.builder().at(x, y).gap(6, 4).visibleWhen(v);
        row.add(Button.builder().icon(CONFIGS_ICON).size(16, 16)
                .tooltip(Component.literal(e.name() + " configs"))
                .onClick(() -> ConfigScreen.open(e, Minecraft.getInstance().gui.screen())).build());
        ModInfo keysMod = e.iconMod();
        if (KeybinderyBridge.present() && keysMod != null) {
            row.add(Button.builder().icon(KeybinderyBridge.KEYS_SPRITE).size(16, 16)
                    .tooltip(Component.literal(e.name() + " keybinds, in Keybindery"))
                    .onClick(() -> KeybinderyBridge.open(keysMod.id(), Minecraft.getInstance().gui.screen())).build());
        }
        row.add(KeyButton.of(0, 0, e.id()));
        row.add(Checkbox.builder().label(Component.literal("Pause menu"))
                .tooltip(Component.literal("A shortcut to " + e.name() + " at the top of the pause menu"))
                .state(() -> HubState.pause(e.id()), on -> HubState.setPause(e.id(), on)).build());
        row.add(Checkbox.builder().label(Component.literal("Inventory"))
                .tooltip(Component.literal("A shortcut to " + e.name() + " at the top of inventories and containers"))
                .state(() -> HubState.inventory(e.id()), on -> HubState.setInventory(e.id(), on)).build());
        return row.build();
    }

    /**
     * One mod as a card: its icon, name and version, its Description section, and its own controls
     * row when it has settings, all inside the card's frame and across its whole width. The card
     * grows with the section; the panel's reflow moves the cards after it.
     */
    private static void card(Body body, ModInfo m) {
        HubEntry entry = new HubEntry.Single(m);
        boolean desc = !m.description().isBlank();
        boolean controls = m.configurable();
        body.row((y, v) -> {
            List<PanelElement> out = new ArrayList<>();
            Section section = desc ? descriptionSection(ModCard.PAD, y + ModCard.SECTION_Y, m, v) : null;
            out.add(new ModCard(y, m.id(), m.name(), m.version(), desc, controls,
                    () -> section == null ? 0 : section.extraLayoutHeight(), v));
            if (section != null) out.add(section);
            if (controls) out.add(controlsRow(ModCard.PAD, y + ModCard.controlsY(desc), entry, v));
            return out;
        }, ModCard.height(desc, controls) + 4);
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

# Mod Hub

Mod Hub is one place for every mod you have installed. It lists them all, opens any mod's settings from that list, and lets you pin the mods you use most as buttons at the top of the pause menu and your inventory. With a lot of mods, each one keeps its settings somewhere different, and I wanted one place to reach all of them.

It's an alternative to [Mod Menu](https://modrinth.com/mod/modmenu) for the mod list and to [YACL](https://modrinth.com/mod/yacl) for building settings screens. Both inspired it, and it runs alongside both, so you can keep them installed. I wish both projects well.

Mod Hub is needed on the client. On a server it does nothing by itself, and it's only there so mods that depend on it can load.

### Features

#### Every mod in one list

- A Mod Hub button in the pause menu and on the title screen opens a list of every installed mod.
- A search box narrows the list by name.
- Each mod's page shows its version, authors, description, and links.
- Libraries, Fabric API's modules, and mods with no settings are grouped under Auxiliary at the bottom of the list.

#### Settings in one place

- A wrench button on a mod's page opens its settings.
- Mods built with YACL, Cloth Config, MidnightLib, or Forge Config API Port open in Mod Hub's own settings screen, with the same tabs and controls as every other mod, color, item, and list settings included.
- Most other mods, Iris among them, register their settings screen for Mod Menu, and Mod Hub opens those too, with or without Mod Menu installed.
- Settings save as soon as you change them, and Undo and Redo step back through your changes.

#### Shortcuts

- Any mod with settings can have a shortcut button at the top of the pause menu, at the top of the inventory and other screens with slots, or both.
- Any mod with settings can also have a key that opens them.

#### Suites

- Mods that belong together can be grouped by their authors into a suite. A suite is one entry in the list, and its mods share one settings screen with a tab for each mod.

#### Keybindery

- With [Keybindery](https://modrinth.com/mod/keybindery) installed, each mod's page gets a button that opens the Key Binds menu showing only that mod's keys. For a suite, it shows the keys of every mod in the suite.

### Requirements

- [Fabric API](https://modrinth.com/mod/fabric-api).
- [MenuKit](https://modrinth.com/mod/menukit) 7.4.0 or newer, below 8.0.0. Mod Hub's screens and buttons are built with it.

### Pairs well with

- [Keybindery](https://modrinth.com/mod/keybindery) 1.2.2 or newer, for the keys button on each mod's page. Mod Hub won't load next to an older version of Keybindery.

### Compatibility

- Minecraft 26.3 and Fabric Loader 0.15.0 or newer.
- Required on the client and optional on the server.
- Mod Hub doesn't change Mod Menu. Mod Menu's Mods button and mod list keep working, so you can use both.

### How to use

#### Opening the mod list

- The pause menu's row of small square buttons has one more at its end, a grass block with the tooltip "Mod Hub". Click it to open the mod list.
- The title screen's row of small square buttons has the same button at its end.

#### The mod list

- The sidebar lists Mod Hub first, then every mod and suite in alphabetical order, then Auxiliary last. Each entry shows the mod's own icon beside its name when the mod has one.
- The Search box at the top of the sidebar narrows the list by name. A suite stays in the list when the search matches the suite's name or the name of any mod in it. Picking a suite while searching scrolls its page to the first mod whose name matches.
- A mod's page shows its icon, name, version, authors, and description, then a button for each link the mod provides (Homepage, Source, Report an issue, and any others it declares for Mod Menu, such as Discord). Clicking a link asks you to confirm before it opens in your browser.
- A suite's page shows the suite's name and how many of its mods are installed, then a box for each mod with its icon, name, and version. Click a box's Description line to show the mod's full description and links, and click it again to hide them.
- Auxiliary's page lists every mod with no settings of its own, in the same boxes. These mods have no controls.
- Minecraft itself is in the list, and its wrench opens the vanilla Options screen.
- Done at the bottom, or Escape, closes the list and returns to the screen you opened it from.

#### The controls on each page

Every mod or suite with settings has a row of controls under its header. In a suite, the suite has its own row and each mod's box has another.

| Control | What it does |
| --- | --- |
| Wrench (tooltip "*name* configs") | Opens the settings for that mod or suite. |
| Keys button (only with Keybindery installed) | Opens Keybindery's Key Binds menu showing only that mod's keys, or every key in the suite. |
| Key button ("Key: None" until set) | Left-click it, then press a key to bind it. Escape cancels without changing anything. Backspace, Delete, or a right-click on the button clears the key. |
| Pause checkbox | Adds a shortcut button for that mod or suite at the top of the pause menu. Off by default. |
| Inventory checkbox | Adds a shortcut button for that mod or suite at the top of the inventory and every other screen with slots. Off by default. |

A mod with no settings shows "No settings." instead of the row.

#### The settings screen

- The screen opens with the mod's or suite's name across the top, "< Back" in the top left corner, and the Mod Hub button in the top right corner. Back, Done, and Escape all return to the screen you opened the settings from. The Mod Hub button opens the mod list.
- A single mod's settings fill the screen with no tab column. When the mod's settings library splits them into categories, each category gets a tab on the left.
- A suite has a tab on the left for each of its mods that has settings. Opening the settings of one mod in a suite opens the suite's screen on that mod's tab.
- Each page starts with the mod's name and description, then a Reset to Defaults button when the mod supports it. Reset to Defaults asks you to confirm, then puts every setting on that tab back to its default.
- Each setting is one row. Hover over a setting's name to see its description. Settings that need a restart say so under the row.
- A setting that's a list shows a row for each item, with up, down, and remove buttons beside the item, and an + Add button that adds one more at the end. The list's name sits on the same line as its buttons. Add grays out when the list is at its longest, and remove when it's at its shortest.
- Every change saves as soon as you make it. There is no Save button.
- A mod built with YACL, Cloth Config, MidnightLib, or Forge Config API Port that has a setting Mod Hub can't draw yet opens its own settings screen instead. Inside a suite, that mod's tab has an Open settings button that opens its own screen.
- To always see each mod's own settings screen instead of Mod Hub's page, turn off "Other mods' settings in Mod Hub's style" in Mod Hub's settings.

#### Undo and Redo

- Undo and Redo are the two arrow buttons to the left of Done. Each one is grayed out when there's nothing to undo or redo, and its tooltip names the setting it would change back, such as "Undo Default look".
- Ctrl+Z undoes and Ctrl+Shift+Z redoes (Cmd+Z and Cmd+Shift+Z on a Mac). These keys aren't rebindable, and they don't apply while you're typing in a text field, which keeps its own undo.
- The history covers every mod's settings and every time you open a settings screen, and it clears when you leave the world.
- Changes made within one second of each other to the same setting, such as dragging a slider, count as one step. Quick edits to the same list join up the same way. Reset to Defaults is also one step.
- A change made in another library's own widget, shown inside a Mod Hub row, isn't recorded in the history.

#### Shortcut buttons

- Shortcut buttons sit in a row at the top center of the pause menu, or of the inventory and other screens with slots, depending on which checkboxes you ticked. The row wraps onto a second line when it runs out of width.
- Each button shows the mod's icon, or its initials when the mod has no icon. Its tooltip is the mod's or suite's name.
- Click a shortcut button to open that mod's or suite's settings. Escape returns to the screen you were on.
- Buttons appear in the order you turned their shortcuts on.

#### Shortcut keys

- A key set with the Key button opens that mod's or suite's settings while you're in a world with no screen open. It doesn't work while chat, the inventory, or another menu is open.
- Shortcuts and keys are saved in `config/modhub/list.json`. A shortcut or key for a mod you remove is kept, so it comes back if you install the mod again.

#### Mod Hub's settings

Mod Hub is the first entry in the mod list, and its wrench opens these settings. They save to `config/modhub/config.json`.

| Setting | Default | What it does |
| --- | --- | --- |
| Default look | On | Mod Hub's screens look like the Options screens. Off, they use the gray Panel look. |
| Other mods' settings in Mod Hub's style | On | Mods whose settings are built with YACL, Cloth Config, MidnightLib, or Forge Config API Port open as Mod Hub pages. Off, each opens its own settings screen. |

### For mod developers

A mod can declare its settings with Mod Hub, and Mod Hub saves them and draws the mod's settings screen. Mods can also be grouped into a suite with a few lines of metadata.

#### Declaring settings

Settings are declared in common code. Each setting is an on/off switch, a whole number in a range, or a choice from an enum, and the screen lists them in the order they're declared.

```java
public static final Settings S = ModHub.settings("mymod");
public static final Setting<Boolean> ENABLED = S.bool("enabled", true);
public static final Setting<Integer> RANGE = S.intRange("range", 8, 1, 32);
public static final Setting<Mode> MODE = S.choice("mode", Mode.NORMAL);
```

Read a value with `ENABLED.get()`. Then point the `modhub` entrypoint at a class that hands the settings over:

```java
public final class MyModHub implements ModHubEntry {
    public Settings settings() { return MyConfig.S; }
}
```

```json
"entrypoints": { "modhub": ["com.example.mymod.MyModHub"] }
```

#### Names and descriptions

Each setting's name comes from the lang key `modhub.<modid>.<key>` and its description from `modhub.<modid>.<key>.desc`. A setting with no lang entry is named from its key, so `railFreePlacement` reads as "Rail free placement".

#### The settings file

Settings are saved to `config/<modid>/config.json` as soon as they change. If the file can't be read, Mod Hub keeps it as a backup and loads the defaults. Keys Mod Hub doesn't recognize are kept when it saves.

#### Suites

To put several mods in a suite, give each of them the same suite id. Any member can name the suite, and one can give it an icon:

```json
"custom": { "modhub": { "suite": "my_suite", "suiteName": "My Suite", "suiteIcon": "assets/mymod/suite.png" } }
```

When members disagree on the name or icon, the member with the alphabetically first mod id wins. A suite with no icon uses its first member's icon. A suite with only one of its mods installed shows as that mod on its own.

### License

MIT. See [LICENSE](https://github.com/trevorschoeny/mod-hub/blob/main/LICENSE).

# Mod Hub

It makes your mods easier to reach. With a lot of mods installed, finding one mod's settings means a trip through menus, and every mod keeps its config screen somewhere different. Mod Hub puts every installed mod in one list, opens a mod's settings in a click, and pins the ones you use most to the top of the pause menu and your inventory as shortcut buttons.

Mod Hub is an alternative to Mod Menu for the mod list, and to YACL for building settings screens. It runs alongside both, so you can keep them installed.

## Features

### A Mod Hub button in the pause menu

The pause menu's row of small square buttons gets one more, a grass block shaped like a jigsaw piece that opens the mod list.

### Every mod in one list

A sidebar lists every installed mod with its icon, and a search box narrows it by name. Pick a mod to see its version, author and description, with Homepage, Source and Report an issue, and Discord buttons when the mod provides those links. Libraries, Fabric API's modules and mods with no settings are grouped under Auxiliary at the bottom, so they don't crowd the list.

### Settings in one click

The wrench button on a mod's page opens its settings. Mods built on Mod Hub get a settings screen that Mod Hub draws for them. Most other mods, Iris, Inventory Plus and YACL-based mods among them, register their config screen for Mod Menu, and Mod Hub opens those too, with or without Mod Menu installed. A few mods rely on more of Mod Menu than their config screen; without Mod Menu, those sit in Auxiliary with no wrench.

### Shortcuts

For each mod or mod suite, you can enable a shortcut button to appear at the top of the pause menu or inventory screen that takes you to that mod’s config. You can also set a key bind to open a mod’s config.

### Suites

Mod authors can group mods that belong together into a suite. The suite is grouped together in the mod list, and their configs are grouped into one screen with tabs on the left side.

### Keybindery

With [Keybindery](https://modrinth.com/mod/keybindery) installed, a mod that has key binds gets a Keybinds button on its page. It opens the Key Binds menu showing only that mod's keys. For a suite, it shows the keys of every mod in it.

## Install

Drop the jar in your `mods/` folder along with Fabric API and MenuKit.

Requires:

- Minecraft 26.3 (this build)
- Fabric Loader 0.15.0 or newer
- Fabric API
- [MenuKit](https://modrinth.com/mod/menukit) 7.1.0 or newer, below 8.0.0

Recommended:

- [Keybindery](https://modrinth.com/mod/keybindery), for the Keybinds button. Mod Hub needs Keybindery 1.2.2 or newer, and won't load next to an older version.

## Adding your mod

A mod declares its settings in common code, and Mod Hub saves them and draws its settings screen. Each setting is an on/off switch, a whole number in a range, or a choice from an enum:

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

Each setting's name comes from the lang key `modhub.<modid>.<key>` and its description from `modhub.<modid>.<key>.desc`. A setting with no lang entry is named from its key, so `railFreePlacement` reads as "Rail free placement".

Settings are saved to `config/<modid>/config.json` as soon as they change. If the file can't be read, Mod Hub keeps it as a backup and loads the defaults. Keys it doesn't recognise are kept when it saves.

To put several mods in a suite, give each of them the same suite id. Any member can name it, and one can give it an icon:

```json
"custom": { "modhub": { "suite": "my_suite", "suiteName": "My Suite", "suiteIcon": "assets/mymod/suite.png" } }
```

A suite with only one of its mods installed shows as that mod on its own.

## Compatibility

Mod Hub installs on both client and server. On a server it does nothing by itself. It's there so mods that depend on it can load.

It doesn't change Mod Menu. Mod Menu's Mods button and mod list keep working, so you can use both.

## License

MIT. See [LICENSE](https://github.com/trevorschoeny/mod-hub/blob/main/LICENSE).

## Credits

Mod Hub was inspired by [Mod Menu](https://modrinth.com/mod/modmenu), for the mod list, and [YACL](https://modrinth.com/mod/yacl), for settings declared in code. We wish both projects well.

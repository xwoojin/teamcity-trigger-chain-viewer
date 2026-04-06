# Trigger Chain View

A TeamCity plugin that visualizes **Finish Build Trigger** chains for each build configuration.

When builds are connected via Finish Build Triggers (e.g., Build A triggers Build B, Build B triggers Build C), this plugin shows the **entire downstream chain** as a tree view — making it easy to understand the full trigger flow at a glance.

## Screenshot

![Trigger Chain View](docs/images/build-structure.png)

## Features

- **Downstream Trigger Chain** — See all builds that are triggered (directly or indirectly) when a build finishes
- **Recursive Tree View** — Chains are displayed as an expandable tree, showing the full depth of trigger relationships
- **Full Project Path** — Each build displays its complete project hierarchy (e.g., `Release :: Client :: KR :: Android :: Build A`)
- **Circular Reference Detection** — Circular trigger chains are detected and marked to prevent infinite loops
- **Expand / Collapse All** — Quickly expand or collapse the entire tree
- **Supports Multiple Trigger Types**
  - Built-in TeamCity **Finish Build Trigger**
  - [Finish Build Trigger (Plus)](https://github.com/xwoojin/teamcity-finish-build-trigger-plus) plugin

## How It Works

Consider this trigger setup:

```
Build A (finishes)
  ├── triggers → Build AAA
  │                └── triggers → Build BBB
  └── triggers → Build B
                   └── triggers → Build C
                                    └── triggers → Build D
```

When you open **Build A** and click the **"Trigger Chain"** tab, you'll see the full downstream chain:

```
Release :: Client :: KR :: Android :: Build A       CURRENT
  ├── Release :: Client :: KR :: IOS :: Build AAA
  │     └── Release :: Client :: KR :: WINDOW :: Build BBB
  └── Dev :: NewOne :: Build B
        └── AnotherDev :: OldOldNew :: Build C
              └── PM :: Piel :: Build D
```

The plugin scans **all build configurations** for Finish Build Triggers that depend on the current build, then recursively follows the chain to build the full tree.

## Installation

1. Download the latest `trigger-chain-view.zip` from the [Releases](https://github.com/xwoojin/teamcity-trigger-chain-viewer/releases) page
2. Go to **TeamCity Administration → Plugins**
3. Click **Upload plugin zip** and select `trigger-chain-view.zip`
4. Enable the plugin and restart TeamCity server if prompted

## Building from Source

### Prerequisites

- Java 11+
- Maven 3.x
- TeamCity server installation (for local lib dependencies)

### Build

Update `teamcity.home` in `pom.xml` to point to your TeamCity server installation, then:

```bash
mvn clean verify
```

The plugin ZIP will be created at: `dist/trigger-chain-view.zip`

## Compatibility

- **TeamCity**: 2025.11+ (build 208117+)

## Version Format

`YYMMDD.N` — where `YY` = year, `MM` = month, `DD` = day, `N` = build number of the day.

## License

MIT License

## Author

**WooJin Kim** — [GitHub](https://github.com/xwoojin)

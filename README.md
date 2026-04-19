# Trigger Chain View

A TeamCity plugin that visualizes **Finish Build Trigger** chains for each build configuration.

When builds are connected via Finish Build Triggers (e.g., Build A triggers Build B, Build B triggers Build C), this plugin shows the **entire downstream chain** as a tree view — making it easy to understand the full trigger flow at a glance.

Works with the built-in TeamCity **Finish Build Trigger** and the [Finish Build Trigger (Plus)](https://github.com/xwoojin/teamcity-finish-build-trigger-plus) plugin.

## Screenshot

![Trigger Chain View](docs/images/build-structures.png)

## Features

- **Trigger Chain tab** — For each build configuration, see every downstream build that is triggered (directly or indirectly) when it finishes, rendered as an expandable tree.
- **Trigger Usage tab** — The inverse view: lists every build that has a Finish Build Trigger watching *this* build (direct usages only). Useful for answering "who depends on me?"
- **Project-level view** — Open any project's **Trigger Chain** tab to see all chains across that project and its sub-projects at once.
- **Full project path** — Each node shows its complete project hierarchy (e.g., `Release :: Client :: KR :: Android :: Build A`).
- **Multi-watch AND grouping** — When several sibling builds are co-watched by the same AND-condition trigger (Finish Build Trigger (Plus) multi-build), the siblings are bundled into a highlighted **Condition group** box, and the shared downstream builds are pulled up as the group's children. This replaces verbose `Condition: X + Y + ...` labels with a clean visual grouping whenever possible.
- **Agent mode badges** — `All Agents` (green) and `Same Agent` (orange) badges surface the FBT+ trigger options at a glance.
- **Circular reference detection** — Circular trigger chains are detected and marked to prevent infinite loops.
- **Expand / Collapse all** — Quickly expand or collapse the entire tree.
- **Deleted builds are auto-hidden** — If a watched build configuration is deleted, it is silently dropped from the view rather than showing a broken entry.

## How It Works

Consider this trigger setup where **Android AAB Signing** and **Android APK Signing** both need to finish before the upload builds can start:

```
Build Android Shipping (finishes)
  ├── triggers → Android AAB to APK Converter
  │                ├── triggers → Android AAB Signing ─┐
  │                │                                   ├─AND→ AAB Playconsole Upload
  │                │                                   ├─AND→ APK Sideload Upload
  │                └── triggers → Android APK Signing ─┘
```

Open **Build Android Shipping** and click the **Trigger Chain** tab:

```
▶ Release :: … :: Build Android Shipping   CURRENT
  ▼ Release :: … :: Android AAB to APK Converter           [Same Agent]
    ┌─ CONDITION (ALL MUST FINISH) ──────────────────────┐
    │ ▶ Release :: … :: Android AAB Signing  [Same Agent]│
    │ ▶ Release :: … :: Android APK Signing  [Same Agent]│
    └──┬─────────────────────────────────────────────────┘
       ├─ Android AAB Playconsole Upload   [Same Agent]
       └─ Android APK Sideload Upload      [Same Agent]
```

The two signing builds are grouped because a single AND-condition trigger covers both, and the two uploads that depend on the group are shown once beneath it.

Open **Android APK Signing** and click the **Trigger Usage** tab to see the inverse — "what watches me":

```
┌─ CONDITION (ALL MUST FINISH): Android AAB Signing + Android APK Signing ─┐
│ ▶ Android AAB Playconsole Upload   [Same Agent]                          │
│ ▶ Android APK Sideload Upload      [Same Agent]                          │
└──────────────────────────────────────────────────────────────────────────┘
```

## Installation

1. Download the latest `trigger-chain-view.zip` from the [Releases](https://github.com/xwoojin/teamcity-trigger-chain-viewer/releases) page.
2. Go to **TeamCity Administration → Plugins**.
3. Click **Upload plugin zip** and select `trigger-chain-view.zip`.
4. Enable the plugin and restart the TeamCity server if prompted.

## Building from Source

### Prerequisites

- Java 11+
- Maven 3.x
- TeamCity server installation (for local lib dependencies)

### Build

Update `teamcity.home` in `pom.xml` to point to your TeamCity server installation, then:

```bash
./build.sh
```

This will automatically increment the version (`YYMMDD.N`) and build the plugin ZIP.

The plugin ZIP will be created at: `dist/trigger-chain-view.zip`

## Compatibility

- **TeamCity**: 2025.11+ (build 208117+)

## Version Format

`YYMMDD.N` — where `YY` = year, `MM` = month, `DD` = day, `N` = build number of the day.

## License

MIT License

## Author

**WooJin Kim** — [GitHub](https://github.com/xwoojin)

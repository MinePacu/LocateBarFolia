# LocateBarFolia

[한국어 문서](README.ko.md)

LocateBarFolia is a Folia-compatible Paper plugin that shows nearby players through Minecraft's tracked waypoint display. Java Edition players receive native waypoint packets, while Bedrock players connected through Floodgate can receive a compact action bar fallback.

## Features

- Shows nearby enabled players as locator waypoints.
- Supports Folia region scheduling.
- Stores each player's enabled or disabled preference.
- Filters targets by world, radius, spectator mode, and optional vanish visibility.
- Provides a Floodgate-based Bedrock fallback through the action bar.
- Supports live configuration reloads and scan radius changes.

## Requirements

- Java 21
- A Minecraft server compatible with Paper/Folia API `1.21`
- Built against Paper dev bundle `1.21.8-R0.1-SNAPSHOT`
- Optional: Floodgate and Geyser for Bedrock fallback support

## Build

```sh
./gradlew build
```

The plugin jar is generated at:

```text
app/build/libs/LocateBarFolia-1.0.0.jar
```

## Installation

1. Build the plugin or download the jar.
2. Place `LocateBarFolia-1.0.0.jar` in your server's `plugins` directory.
3. Restart the server.
4. Edit `plugins/LocateBarFolia/config.yml` if needed.
5. Run `/locatebar reload` after changing the config.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/locatebar` | `locatebar.use` | Toggles your LocateBar participation. |
| `/locatebar on` | `locatebar.use` | Enables your participation. |
| `/locatebar off` | `locatebar.use` | Disables your participation. |
| `/locatebar toggle` | `locatebar.use` | Toggles your participation. |
| `/locatebar radius` | `locatebar.admin` | Shows the current scan radius. |
| `/locatebar radius <blocks>` | `locatebar.admin` | Updates and saves the scan radius. Values below 8 are clamped to 8. |
| `/locatebar reload` | `locatebar.admin` | Reloads the plugin configuration. |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `locatebar.use` | `true` | Allows players to use participation commands. |
| `locatebar.admin` | `op` | Allows radius changes and configuration reloads. |

## Configuration

```yaml
enabled-by-default: true
scan-radius: 48.0
movement-threshold-blocks: 0.5
ignore-spectators: true
ignore-vanished: false
bedrock-fallback:
  enabled: true
  actionbar-interval-ticks: 20
  max-targets: 4
```

| Option | Description |
| --- | --- |
| `enabled-by-default` | Whether players participate by default when no saved preference exists. |
| `scan-radius` | Maximum target detection radius in blocks. Invalid values reset to `48.0`; values below `8.0` are clamped. |
| `movement-threshold-blocks` | Minimum movement needed before refreshing target visibility. Values below `0.05` are clamped. |
| `ignore-spectators` | Excludes spectators from being recipients or targets. |
| `ignore-vanished` | When enabled, only shows targets that the recipient can see through Bukkit visibility checks. |
| `bedrock-fallback.enabled` | Enables action bar fallback for Floodgate Bedrock players. |
| `bedrock-fallback.actionbar-interval-ticks` | Action bar refresh interval. Values below `10` are clamped. |
| `bedrock-fallback.max-targets` | Maximum Bedrock fallback targets shown at once. Values below `1` are clamped. |

## Bedrock Fallback

If Floodgate is installed, LocateBarFolia detects Bedrock players through the Floodgate API. Bedrock players do not receive the Java waypoint packets; instead, they receive an action bar message such as:

```text
Locate: F Alex 10m | L Steve 20m
```

Direction codes are relative to the recipient's current facing direction:

| Code | Meaning |
| --- | --- |
| `F` | Front |
| `FR` | Front-right |
| `R` | Right |
| `BR` | Back-right |
| `B` | Back |
| `BL` | Back-left |
| `L` | Left |
| `FL` | Front-left |

If Floodgate is not installed, the plugin still works for Java Edition players and logs that Bedrock fallback is disabled.

## Development Notes

This project uses `io.papermc.paperweight.userdev`, so IDEs must import the Gradle project with the wrapper. In VS Code, use the Java extension pack and reload the Java language server after `./gradlew build` if `net.minecraft` imports are unresolved.

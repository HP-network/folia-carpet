# FoliaCarpet

Folia-native technical controls for Minecraft Java Edition 1.21.11. The project keeps the practical, rule-driven workflow people expect from Carpet while using Folia's region-safe scheduling model instead of Fabric mixins or a main-thread assumption.

This is an independent Bukkit/Paper plugin, not a fork or a replacement for [Fabric Carpet](https://github.com/gnembon/fabric-carpet). It does not copy Carpet's Fabric implementation and it does not provide Scarpet, client-side features, or Fabric-only mixins.

## What it does

- Adds `/carpet list`, `/carpet set`, `/carpet lang`, and `/carpet status`.
- Applies a per-world entity spawn budget without touching another region's entity state.
- Applies a per-second redstone event budget at the event boundary.
- Enforces view and simulation distance from the global region scheduler.
- Uses Folia's `RegionScheduler`-compatible event boundaries and `GlobalRegionScheduler` for global work.
- Ships with Chinese (`zh-CN`) and English (`en-US`) messages. Chinese is the default.
- Declares `folia-supported: true` and targets the Folia 1.21.11 API.

## Requirements

- Folia 1.21.11
- Java 21

The plugin is deliberately version-pinned. Folia's region ownership and API contracts can change between Minecraft versions; a future version should receive its own build and verification before being advertised as compatible.

## Install

1. Build with `./gradlew build`.
2. Copy `build/libs/folia-carpet-0.1.0.jar` to the Folia `plugins` directory.
3. Start the server once, then edit `plugins/FoliaCarpet/config.yml` if needed.

## Commands

```text
/carpet list
/carpet set <rule> <value>
/carpet lang <zh-CN|en-US>
/carpet status
```

Rules:

| Rule | Default | Range | Purpose |
| --- | ---: | ---: | --- |
| `entity-spawn-cap` | 64 | 1-10000 | Limits accepted entity spawn events tracked by the plugin per world. |
| `redstone-events-per-second` | 250 | 1-10000 | Stops a redstone storm from consuming unbounded event work in one second. |
| `max-view-distance` | 10 | 2-32 | Caps each world's view distance. |
| `max-simulation-distance` | 8 | 2-32 | Caps each world's simulation distance. |
| `language` | `zh-CN` | `zh-CN`, `en-US` | Selects command messages and diagnostics. |

Changes are saved to `config.yml` immediately. Use `/reload` only if another plugin requires it; FoliaCarpet does not depend on a global reload.

## Threading model

Entity and block events execute in the region that owns the affected location. The plugin only updates independent counters in those callbacks. Periodic distance enforcement and diagnostics run on Folia's global region scheduler. No Bukkit scheduler, synchronous task, Fabric loader, mixin, or static world scan is used.

The counters are intentionally conservative: they protect a server from bursts, but they are not a replacement for a full mob-cap implementation. Set the limits to match the server's gameplay and test them with the actual datapacks and plugins in use.

## Build and test

```sh
./gradlew test
./gradlew build
```

The GitHub Actions workflow runs the same Java 21 build and test commands.

## License and relationship

FoliaCarpet is released under the MIT License. Carpet is a separate project by gnembon and contributors. This project is an independent Folia-oriented plugin and is not affiliated with or endorsed by the Carpet maintainers.

# FoliaCarpet

FoliaCarpet is an independent Paper-style plugin port of the Carpet 1.4.194 server feature set for **Folia 1.21.11**. It keeps Carpet's rule registry, `/carpet` settings workflow, command modules, loggers, profiler, fake-player/action-pack support, and Scarpet runtime, while routing the Folia-facing work through region and global schedulers.

This repository is not a GitHub fork. It keeps the upstream Carpet MIT license and attribution.

## Status

The project contains the complete Carpet 1.4.194 Java/runtime source tree and the Folia adapter used by the plugin. The plugin starts on a stock Folia 1.21.11 server and enables the Folia-safe implementation paths.

Minecraft internals that Carpet implements exclusively through Fabric Mixin cannot be made active by a normal Bukkit plugin after Folia has loaded its server classes. Those paths remain in the source tree for parity and API compatibility, but require a patched Folia server build. They are not advertised as active in the stock-plugin mode.

## Included

- Carpet rule metadata, validation, persistence, language lookup, and interactive `/carpet` commands.
- Carpet commands including `/counter`, `/distance`, `/draw`, `/info`, `/log`, `/mobai`, `/perimeterinfo`, `/player`, `/profile`, `/spawn`, `/script`, and the test command where the server API permits them.
- Scarpet parser, evaluator, standard APIs, app store support, event runtime, shapes, and bundled scripts.
- Carpet logging/HUD helpers, counters, profiling hooks, fake players, and action packs.
- Folia region-safe adapters for TNT handling, movable block entities, persistent parrots, stackable shulker boxes, rail power limits, chunk tracking, and server lifecycle events.
- English and Simplified Chinese language resources from Carpet, with the normal Carpet language rule preserved.

## Requirements

- Folia 1.21.11 (Paper is not supported by this build).
- Java 21.

The version is intentionally pinned. Do not install this jar on another Minecraft/Folia version without rebuilding and testing against that server version.

## Build

The NMS compile classpath is generated from a pinned Folia source commit. The server jar is a local build dependency and is not committed to Git.

The script respects the proxy environment already configured on the machine. In a restricted network, export the proxy variables shown below before running it; CI uses its normal direct network connection.

```sh
export https_proxy=http://127.0.0.1:7890
export http_proxy=http://127.0.0.1:7890
export all_proxy=socks5://127.0.0.1:7890

./scripts/fetch-folia-server.sh
./gradlew clean test build --no-daemon
```

The output is `build/libs/folia-carpet-1.4.194-folia.1.jar`.

Set `FOLIA_SOURCE_DIR` to an existing checkout when building repeatedly. Set `FOLIA_SERVER_JAR` to a compatible development/remapped Folia server jar when using a prebuilt classpath.

## Install

1. Build the jar with the commands above.
2. Put the jar in the Folia server's `plugins` directory.
3. Restart the server and configure rules with `/carpet`.

Back up worlds before enabling experimental rules. Folia region ownership means a rule that is safe on a single-threaded server can still need a region-aware implementation.

## Verification

The repository CI runs the Java 21 build after generating the pinned Folia development jar. Local smoke testing was performed on a clean Folia 1.21.11 server: the plugin was discovered as a Folia-supported Paper plugin, enabled, loaded a new world, and shut down without plugin exceptions.

## License

MIT. See [`LICENSE`](LICENSE) and [`NOTICE.md`](NOTICE.md) for attribution.

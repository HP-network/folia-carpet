# FoliaCarpet

FoliaCarpet is the Carpet 1.4.194 runtime adapted for Folia 1.21.11. It adds Carpet rules, Scarpet scripting, fake players, loggers, profiling, spawn tools and technical-server utilities without requiring a client-side mod.

The plugin keeps Carpet's command and rule model while routing world, entity and tick work through Folia's region schedulers where required.

## Features

- Carpet rule system with command, survival, creative, feature, optimization, bugfix, TNT, dispenser, Scarpet, client and experimental categories
- Scarpet expressions, app loading, events, custom commands and bundled scripts
- Fake players and action packs: movement, looking, mining, attacking, item use, inventory actions and more
- Hopper counters, block information, shape drawing, spawn inspection and mob-cap tools
- Server health and entity profiling
- Logger subscriptions and in-game overlays
- Mob AI tracking with `/track`
- Folia region-aware scheduling for supported operations

## Compatibility

- Folia 1.21.11
- Java 21 JDK (a JRE or stripped runtime is not enough for JVM instrumentation)
- Server plugin only; clients do not need Fabric, Quilt or a separate mod
- Not compatible with ordinary Paper, Spigot, Fabric or Quilt servers

If the hosting panel or JVM reports `Can't attach to this VM`, add the plugin as a startup agent. Put this option before `-jar` (or in the panel's JVM arguments):

```text
-javaagent:plugins/folia-carpet-1.4.194-folia.2.jar
```

For example on Windows:

```bat
java -javaagent:plugins/folia-carpet-1.4.194-folia.2.jar -jar folia-server.jar --nogui
```

The dynamic Attach path is still used automatically when the server permits it.

Use one copy of FoliaCarpet per server. Back up the world before enabling rules that change redstone, TNT, block movement or other game mechanics.

## Installation

1. Stop the Folia server.
2. Download `folia-carpet-1.4.194-folia.2.jar` from [Releases](https://github.com/HP-network/folia-carpet/releases).
3. Copy the jar into the server's `plugins` directory.
4. Start the server and check the startup log for `Carpet Folia initialized` and `Carpet Folia enabled`.
5. Run `/carpet list` as an operator or from the console.

The first rule change creates or updates `carpet.conf` in the world directory. Do not edit that file while the server is running.

## Commands

| Command | Purpose |
| --- | --- |
| `/carpet` | List, inspect and change Carpet rules. |
| `/counter` | View and reset hopper counters. |
| `/distance` | Measure the distance between positions. |
| `/draw` | Draw spheres, balls, diamonds, pyramids, cones, cylinders and cuboids. |
| `/info` | Inspect block information. |
| `/log` | List, enable, configure and stop Carpet loggers. |
| `/perimeterinfo` | Inspect spawnable spaces around a position. |
| `/player` | Spawn and control fake players and action packs. |
| `/profile` | Profile server health or entities. |
| `/script` | Run, load, unload and manage Scarpet scripts. |
| `/spawn` | Inspect spawn attempts, mob caps, rates and entities. |
| `/track` | Track mob AI goals for an entity type. |

Use Minecraft's tab completion for the complete subcommand tree. Useful examples:

```text
/carpet list
/carpet commandPlayer ops
/carpet hopperCounters true
/carpet language zh_cn
/profile health 200
/profile entities 200
/spawn mobcaps
/spawn tracking start
/player Steve spawn
/player Steve move forward
/log
/script run 1 + 1
```

## Rules and permissions

Rules are grouped into the categories shown by `/carpet list`. Use the following forms to inspect the exact description, current value, default and allowed options:

```text
/carpet <rule>
/carpet list <category>
```

Command access follows Minecraft permission levels. The relevant `command...` rules control individual Carpet commands. For example:

```text
/carpet commandLog true
/carpet commandPlayer 2
/carpet commandScriptACE 4
```

Scarpet code that can execute commands or modify the world is controlled separately by `commandScriptACE`. Fake players using offline profiles also require `allowSpawningOfflinePlayers`.

## Language

The default language is English. Change it with:

```text
/carpet language zh_cn
```

Bundled language packs: English, Simplified Chinese, Traditional Chinese, French, Spanish (Argentina) and Portuguese (Brazil).

## Build from source

The build needs a Java 21 JDK and a Folia 1.21.11 server jar. If the server jar is not already present, fetch and build it first:

```bash
./scripts/fetch-folia-server.sh
./gradlew clean build --no-daemon
```

The plugin is written to:

```text
build/libs/folia-carpet-1.4.194-folia.2.jar
```

To use a different local server jar, set `FOLIA_SERVER_JAR` before running Gradle.

## License

MIT. See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md) for the project and upstream attribution.

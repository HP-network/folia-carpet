# FoliaCarpet

Carpet utilities and Scarpet scripting for Folia 1.21.11. FoliaCarpet brings Carpet rules, technical-server commands, fake players, logging, profiling and the Scarpet runtime to a Folia server.

## Installation

1. Download the latest `folia-carpet` jar from [Releases](https://github.com/HP-network/folia-carpet/releases).
2. Put the jar in the server's `plugins` directory.
3. Start or restart the server.
4. Run `/carpet list` in the console or as an operator to check the available rules.

FoliaCarpet is a Folia plugin. It is not intended for a regular Paper or Spigot server.

## Usage

Carpet rules are changed with `/carpet`:

```text
/carpet list
/carpet <rule>
/carpet <rule> <value>
```

Examples:

```text
/carpet commandPlayer true
/carpet hopperCounters true
/carpet language zh_cn
```

Rule values are saved in the world's `carpet.conf` file. A rule can be reset with its default value shown by `/carpet <rule>`.

## Commands

| Command | Purpose |
| --- | --- |
| `/carpet` | List and change Carpet rules. |
| `/counter` | View and reset hopper counters. |
| `/distance` | Measure the distance between positions. |
| `/draw` | Draw spheres, balls, diamonds, pyramids, cones, cylinders and cuboids. |
| `/info` | Inspect block information. |
| `/log` | Enable, configure and stop Carpet loggers. |
| `/mobai` | Inspect and track mob AI goals. |
| `/perimeterinfo` | Inspect spawnable spaces around a position. |
| `/player` | Spawn and control fake players and action packs. |
| `/profile` | Profile server health and entities. |
| `/script` | Run and manage Scarpet scripts. |
| `/spawn` | Inspect spawn attempts, mob caps and spawn rates. |
| `/track` | Track mob AI goals. |

Some commands are enabled or restricted by their corresponding Carpet rule. Use `/carpet list command` to see the command rules on the server.

## Rules

Rules are grouped into `COMMAND`, `CREATIVE`, `FEATURE`, `SURVIVAL`, `TNT`, `SCARPET`, `OPTIMIZATION`, `BUGFIX` and `EXPERIMENTAL`. Common examples include:

- `commandPlayer`, `commandScript`, `commandLog` and `commandDraw`
- `hopperCounters`, `persistentParrots` and `renewableCoral`
- `mergeTNT`, `optimizedTNT` and `tntDoNotUpdate`
- `movableBlockEntities`, `shulkerBoxStackSize` and `railPowerLimit`
- `creativeNoClip`, `antiCheatDisabled` and `lagFreeSpawning`

Run `/carpet list <category>` or `/carpet <rule>` for the exact value, description and allowed options for a rule.

## Permissions

FoliaCarpet follows Minecraft operator permission levels. The default command rule is `ops`. Administrators can change command access with the relevant `command...` rule, for example:

```text
/carpet commandLog true
/carpet commandPlayer 2
/carpet commandScriptACE 4
```

Only an operator with the required level can raise or lower a command's permission level. Fake-player commands also require `allowSpawningOfflinePlayers` when an offline profile is used.

## Language

The default language is English. Switch the server language with:

```text
/carpet language zh_cn
```

Available language packs include English, Simplified Chinese, Traditional Chinese, French, Spanish (Argentina) and Portuguese (Brazil).

## Requirements

- Folia 1.21.11
- Java 21

## License

MIT. See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md).

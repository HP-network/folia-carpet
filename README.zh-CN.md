# FoliaCarpet

面向 Folia 1.21.11 的 Carpet 工具和 Scarpet 脚本插件，提供 Carpet 规则、技术服命令、假人、日志、性能分析和 Scarpet 运行时。

## 安装

1. 从 [Releases](https://github.com/HP-network/folia-carpet/releases) 下载最新的 `folia-carpet` jar。
2. 将 jar 放入服务端的 `plugins` 目录。
3. 启动或重启服务端。
4. 在控制台或管理员账号中执行 `/carpet list` 查看规则。

FoliaCarpet 是 Folia 插件，不适用于普通 Paper 或 Spigot 服务端。

## 使用方法

使用 `/carpet` 查看和修改 Carpet 规则：

```text
/carpet list
/carpet <规则>
/carpet <规则> <值>
```

示例：

```text
/carpet commandPlayer true
/carpet hopperCounters true
/carpet language zh_cn
```

规则会保存到世界目录中的 `carpet.conf`。执行 `/carpet <规则>` 可以查看当前值、默认值和可用选项。

## 命令

| 命令 | 用途 |
| --- | --- |
| `/carpet` | 查看和修改 Carpet 规则。 |
| `/counter` | 查看和重置漏斗计数器。 |
| `/distance` | 测量位置之间的距离。 |
| `/draw` | 绘制球体、圆球、菱形、金字塔、圆锥、圆柱和长方体。 |
| `/info` | 查看方块信息。 |
| `/log` | 启用、配置和停止 Carpet 日志器。 |
| `/mobai` | 查看和跟踪生物 AI 目标。 |
| `/perimeterinfo` | 检查位置周围可刷怪区域。 |
| `/player` | 生成和控制假人及动作包。 |
| `/profile` | 分析服务端状态和实体。 |
| `/script` | 运行和管理 Scarpet 脚本。 |
| `/spawn` | 查看刷怪尝试、怪物上限和刷怪率。 |
| `/track` | 跟踪生物 AI 目标。 |

部分命令会受对应的 Carpet 规则控制。执行 `/carpet list command` 可以查看当前的命令规则。

## 规则配置

规则分为 `COMMAND`、`CREATIVE`、`FEATURE`、`SURVIVAL`、`TNT`、`SCARPET`、`OPTIMIZATION`、`BUGFIX` 和 `EXPERIMENTAL`。常用示例：

- `commandPlayer`、`commandScript`、`commandLog`、`commandDraw`
- `hopperCounters`、`persistentParrots`、`renewableCoral`
- `mergeTNT`、`optimizedTNT`、`tntDoNotUpdate`
- `movableBlockEntities`、`shulkerBoxStackSize`、`railPowerLimit`
- `creativeNoClip`、`antiCheatDisabled`、`lagFreeSpawning`

执行 `/carpet list <分类>` 或 `/carpet <规则>` 查看准确的说明、当前值和可用选项。

## 权限

FoliaCarpet 使用 Minecraft 的管理员权限等级。命令规则默认值为 `ops`。管理员可以通过对应的 `command...` 规则调整命令权限，例如：

```text
/carpet commandLog true
/carpet commandPlayer 2
/carpet commandScriptACE 4
```

只有达到要求等级的管理员才能修改命令权限。使用离线玩家档案生成假人时，还需要启用 `allowSpawningOfflinePlayers`。

## 语言

默认语言为英语。切换为简体中文：

```text
/carpet language zh_cn
```

可用语言包括英语、简体中文、繁体中文、法语、阿根廷西班牙语和巴西葡萄牙语。

## 环境要求

- Folia 1.21.11
- Java 21

## 许可证

MIT，见 [LICENSE](LICENSE) 和 [NOTICE.md](NOTICE.md)。

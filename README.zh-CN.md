# FoliaCarpet

FoliaCarpet 是将 Carpet 1.4.194 运行时适配到 Folia 1.21.11 的服务端插件。它提供 Carpet 规则、Scarpet 脚本、假人、日志、性能分析、刷怪分析和技术服工具，不需要客户端安装模组。

插件保留 Carpet 的命令和规则模型，并在需要时通过 Folia 区域调度器执行世界、实体和 tick 操作。

## 功能

- Carpet 规则系统：命令、生存、创造、功能、优化、修复、TNT、发射器、Scarpet、客户端和实验性分类
- Scarpet 表达式、脚本应用、事件、自定义命令和内置脚本
- 假人和动作包：移动、转向、挖掘、攻击、使用物品、背包操作等
- 漏斗计数器、方块信息、形状绘制、刷怪检查和生物上限工具
- 服务端状态和实体性能分析
- 日志订阅和游戏内覆盖显示
- 使用 `/track` 跟踪生物 AI 目标
- 对支持的操作使用 Folia 区域安全调度

## 兼容性

- Folia 1.21.11
- Java 21 完整 JDK（精简 JRE 或裁剪运行时无法完成 JVM 插桩）
- 仅服务端插件，客户端不需要安装 Fabric、Quilt 或其他模组
- 不兼容普通 Paper、Spigot、Fabric 或 Quilt 服务端

每个服务端只放一份 FoliaCarpet。启用会改变红石、TNT、方块移动或其他游戏机制的规则前，请先备份世界。

## 安装

1. 停止 Folia 服务端。
2. 从 [Releases](https://github.com/HP-network/folia-carpet/releases) 下载 `folia-carpet-1.4.194-folia.2.jar`。
3. 将 jar 放入服务端的 `plugins` 目录。
4. 启动服务端，确认启动日志中出现 `Carpet Folia initialized` 和 `Carpet Folia enabled`。
5. 以管理员身份或在控制台执行 `/carpet list`。

第一次修改规则时，插件会在世界目录创建或更新 `carpet.conf`。服务端运行时不要编辑此文件。

## 命令

| 命令 | 用途 |
| --- | --- |
| `/carpet` | 查看、检查和修改 Carpet 规则。 |
| `/counter` | 查看和重置漏斗计数器。 |
| `/distance` | 测量位置之间的距离。 |
| `/draw` | 绘制球体、圆球、菱形、金字塔、圆锥、圆柱和长方体。 |
| `/info` | 查看方块信息。 |
| `/log` | 查看、启用、配置和停止 Carpet 日志器。 |
| `/perimeterinfo` | 检查位置周围的可刷怪区域。 |
| `/player` | 生成和控制假人及动作包。 |
| `/profile` | 分析服务端状态或实体。 |
| `/script` | 运行、加载、卸载和管理 Scarpet 脚本。 |
| `/spawn` | 查看刷怪尝试、怪物上限、刷怪率和实体。 |
| `/track` | 跟踪指定生物类型的 AI 目标。 |

使用 Minecraft 的 Tab 补全查看完整子命令。常用示例：

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

## 规则和权限

规则分类以 `/carpet list` 显示为准。使用以下命令查看规则的准确说明、当前值、默认值和可用选项：

```text
/carpet <规则>
/carpet list <分类>
```

命令权限使用 Minecraft 管理员权限等级。对应的 `command...` 规则可以单独控制 Carpet 命令，例如：

```text
/carpet commandLog true
/carpet commandPlayer 2
/carpet commandScriptACE 4
```

可以执行命令或修改世界的 Scarpet 代码由 `commandScriptACE` 单独控制。使用离线档案生成假人时，还需要启用 `allowSpawningOfflinePlayers`。

## 语言

默认语言为英语。切换为简体中文：

```text
/carpet language zh_cn
```

内置语言包：英语、简体中文、繁体中文、法语、阿根廷西班牙语和巴西葡萄牙语。

## 从源码构建

构建需要 Java 21 完整 JDK 和 Folia 1.21.11 服务端 jar。如果本地还没有服务端 jar，先执行：

```bash
./scripts/fetch-folia-server.sh
./gradlew clean build --no-daemon
```

插件产物位置：

```text
build/libs/folia-carpet-1.4.194-folia.2.jar
```

需要使用其他本地服务端 jar 时，在执行 Gradle 前设置 `FOLIA_SERVER_JAR`。

## 许可证

MIT。项目和上游归属信息见 [LICENSE](LICENSE) 与 [NOTICE.md](NOTICE.md)。

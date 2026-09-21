# FoliaCarpet

FoliaCarpet 是面向 **Folia 1.21.11** 的独立 Paper 风格插件，迁移 Carpet 1.4.194 的服务端功能。项目保留 Carpet 的规则注册、`/carpet` 设置流程、命令模块、日志器、性能分析、假人/动作包和 Scarpet 运行时，并将 Folia 侧操作接入区域调度器与全局调度器。

本仓库不是 GitHub fork，保留上游 Carpet 的 MIT 许可证和版权归属。

## 当前状态

仓库包含完整的 Carpet 1.4.194 Java/运行时源码树，以及插件使用的 Folia 适配层。插件可以在原版 Folia 1.21.11 上被识别、加载和启用，并使用 Folia 安全路径运行。

Carpet 中有一部分功能完全依赖 Fabric Mixin 修改 Minecraft 内部类。普通 Bukkit 插件必须等 Folia 加载完服务端类之后才进入生命周期，因此无法在 stock 插件模式下凭空启用这些 Mixin 路径。相关源码仍保留，用于规则/API 对齐；要让这些路径生效，需要使用打过补丁的 Folia 服务端。文档不会把它们伪装成 stock 插件已经启用。

## 已包含

- Carpet 规则元数据、校验、持久化、语言查找和交互式 `/carpet` 命令。
- `/counter`、`/distance`、`/draw`、`/info`、`/log`、`/mobai`、`/perimeterinfo`、`/player`、`/profile`、`/spawn`、`/script` 以及服务器 API 允许的测试命令。
- Scarpet 解析器、求值器、标准 API、应用商店支持、事件运行时、形状系统和内置脚本。
- Carpet 日志/HUD 辅助类、计数器、性能分析钩子、假人和动作包。
- TNT、可移动方块实体、持久鹦鹉、可堆叠潜影盒、铁轨功率上限、区块跟踪及服务器生命周期的 Folia 区域安全适配。
- Carpet 原有英文和简体中文语言资源，并保留标准 Carpet 语言规则。

## 环境要求

- Folia 1.21.11（此构建不支持普通 Paper）。
- Java 21。

版本被刻意锁定。不要把 jar 直接安装到其他 Minecraft/Folia 版本；应先针对对应版本重新构建并测试。

## 构建

NMS 编译类路径由固定的 Folia 源码 commit 生成。服务端开发 jar 只作为本地构建依赖，不提交到 Git。

脚本会沿用机器上已有的代理环境。网络受限时，请在运行前导出下面的代理变量；CI 使用自身的直连网络。

```sh
export https_proxy=http://127.0.0.1:7890
export http_proxy=http://127.0.0.1:7890
export all_proxy=socks5://127.0.0.1:7890

./scripts/fetch-folia-server.sh
./gradlew clean test build --no-daemon
```

产物是 `build/libs/folia-carpet-1.4.194-folia.2.jar`。

重复构建时可以设置 `FOLIA_SOURCE_DIR` 指向已有 Folia 源码目录；如果已有兼容的开发/重映射 Folia server jar，可以用 `FOLIA_SERVER_JAR` 指定它。

## 安装

1. 按上面的命令构建 jar。
2. 把 jar 放进 Folia 服务端的 `plugins` 目录。
3. 重启服务端，用 `/carpet` 配置规则。

启用实验性规则前先备份世界。Folia 的区域所有权意味着单线程服务端安全的规则，仍可能需要区域线程专用实现。

## 验证

仓库 CI 会先生成固定版本的 Folia 开发 jar，再用 Java 21 执行构建。本地已在干净的 Folia 1.21.11 服务端进行启动烟测：插件被识别为 Folia 支持的 Paper 插件，成功启用、创建新世界并正常关闭，没有出现插件异常。

## 许可证

MIT，见 [`LICENSE`](LICENSE) 和 [`NOTICE.md`](NOTICE.md) 中的版权归属。

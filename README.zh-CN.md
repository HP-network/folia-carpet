# FoliaCarpet

面向 Minecraft Java Edition 1.21.11 Folia 的技术型控制插件。它保留 Carpet 用户熟悉的规则式命令工作流，但按照 Folia 的区域线程模型实现，不使用 Fabric Mixin，也不假设存在单一主线程。

这是独立的 Bukkit/Paper 插件，不是 [Fabric Carpet](https://github.com/gnembon/fabric-carpet) 的 fork 或替代品。项目没有复制 Carpet 的 Fabric 实现，也不提供 Scarpet、客户端功能或 Fabric 专属 Mixin。

## 功能

- 提供 `/carpet list`、`/carpet set`、`/carpet lang`、`/carpet status`。
- 在事件边界限制每个世界接受的实体生成量，不跨区域读取实体状态。
- 限制每秒红石事件预算，避免红石风暴持续占满事件处理时间。
- 通过全局区域调度器约束视距和模拟距离。
- 事件回调遵循 Folia 区域模型，全局工作使用 `GlobalRegionScheduler`。
- 内置简体中文 `zh-CN` 和英文 `en-US`，默认中文。
- 声明 `folia-supported: true`，目标为 Folia 1.21.11 API。

## 要求

- Folia 1.21.11
- Java 21

插件有意锁定版本。Folia 的区域归属和 API 约定可能随 Minecraft 版本变化；升级版本必须单独构建和验证，不能只改配置中的版本号。

## 安装

1. 执行 `./gradlew build`。
2. 将 `build/libs/folia-carpet-0.1.0.jar` 放进 Folia 的 `plugins` 目录。
3. 首次启动后按需修改 `plugins/FoliaCarpet/config.yml`。

## 命令

```text
/carpet list
/carpet set <规则> <值>
/carpet lang <zh-CN|en-US>
/carpet status
```

规则默认值和范围见英文 README 的表格。修改会立即保存到 `config.yml`；FoliaCarpet 不依赖全局 `/reload`。

## 线程模型

实体和方块事件在所属区域执行，插件只在回调中更新独立计数器。周期性的距离约束和诊断在 Folia 全局区域调度器执行。没有 Bukkit 同步任务、Fabric loader、Mixin 或全局静态世界扫描。

计数器采用保守策略，用来保护服务器免受突发事件影响，不等同于完整的原版生物上限实现。请结合服务器实际插件、数据包和玩法压测后调整数值。

## 构建与测试

```sh
./gradlew test
./gradlew build
```

GitHub Actions 会运行同样的 Java 21 构建和测试命令。

## 许可与关系

FoliaCarpet 使用 MIT License。Carpet 是 gnembon 及贡献者维护的独立项目。本项目是独立的 Folia 插件，与 Carpet 维护者没有隶属或背书关系。

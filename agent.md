# EIMusic 仓库协作指南

## 任务分工

- 主线程使用 Codex GPT-6.2 Sol，负责规划、拆分任务和验收。
- 需要实现代码时，主线程通过 `tcode_control.start_thread` 委派，指定 `provider: "codex"`、`model: "gpt-6-luna"`、`subagent: true`。
- 委派消息必须写明修改范围和验收条件。
- 收到委派结果后，主线程审查 diff 并验证是否满足验收条件。

## 模块导航

EIMusic 是基于 Kotlin Multiplatform 和 Compose Multiplatform 的开源音乐播放器。

| 目录 | 职责 |
| --- | --- |
| `shared/` | 共享模型、网络、认证和 SQLDelight 仓储 |
| `composeApp/` | Compose 客户端的共享界面和各平台实现 |
| `androidApp/` | Android 应用入口和 APK 构建 |
| `iosApp/` | iOS 应用入口和 Xcode 工程 |
| `terminalApp/` | 基于 Mosaic 的终端客户端 |
| `server/` | Ktor 服务端，包含认证、房间同步、网易云接口代理和管理端 |

## 构建与验收

- 从仓库根目录使用 Gradle Wrapper。服务端和 CI 使用 JDK 25。Android SDK 要求见 [README](README.md)。
- 终端客户端通过 JitPack 获取 Mosaic。依赖坐标见 [README](README.md)。
- 按改动范围选择验证命令。涉及共享行为时，同时检查受影响的调用方。验收时记录实际运行的命令、目标平台和结果。

| 改动范围 | 常用验证命令 |
| --- | --- |
| 共享核心 | `./gradlew :shared:jvmTest` |
| 服务端 | `./gradlew :server:test :server:buildFatJar` |
| Compose 桌面端 | `./gradlew :composeApp:compileKotlinDesktop` |
| Android | `./gradlew :androidApp:assembleDebug` |
| 终端客户端（Linux） | `./gradlew :terminalApp:compileKotlinLinuxX64` |

完整 CI 命令见 [CI 工作流](.github/workflows/ci.yml)。各平台的运行命令和当前支持状态见 [README](README.md)。

## 文档维护

- 按 [项目维护规矩](docs/PROJECT_RULES.md) 同步功能、交互、构建及发布变更。各模块的跟进文档见 [文档索引](docs/README.md)。
- 更新跟进项时附上验证证据。只验证了某个平台时，标明实际验证的平台。

# EIMusic

开源音乐播放器，基于 Kotlin Multiplatform 构建。

## 模块

- `composeApp` — Compose Multiplatform 共享客户端（Android / iOS / Desktop / Web 的 UI 与逻辑）
- `androidApp` — Android 应用入口（Manifest、启动 Activity、签名与打包配置），依赖 `composeApp`
- `shared` — 各端共享的核心代码（数据模型、网络、认证等）
- `server` — Ktor 服务端，提供房间同步等能力
- `terminalApp` — 基于 Mosaic 的终端（TUI）客户端，支持 Linux / macOS / Windows

## 环境要求

- JDK 17+
- Android SDK（构建 Android 端时需要）
- 构建 Web 端需要 [Kotlin/Wasm](https://kotl.in/wasm/) 支持

### mosaic（编译 terminalApp 的必需依赖）

`settings.gradle.kts` 通过 `includeBuild(...)` 引入本地复合构建，且该行无条件生效——
**找不到 `mosaic` 目录会导致任何 Gradle 构建直接配置失败**。编译/运行前先克隆 mosaic
（脚本会依次尝试 `mosaic`、`../mosaic`、`../../mosaic` 三个位置）：

```bash
git clone https://github.com/SparrowAndSnow/mosaic
```

## 构建与运行

### 服务端

```bash
./gradlew :server:run
```

### 桌面客户端

```bash
./gradlew :composeApp:run
```

打包发行版：

```bash
./gradlew :composeApp:createReleaseDistributable
```

### Web 客户端（开发模式）

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

### Android

使用 Android Studio 打开项目后直接运行，或在 CLI 中执行：

```bash
./gradlew :androidApp:assembleDebug
```

### 终端客户端（terminalApp）

Linux：

```bash
./gradlew :terminalApp:linkReleaseExecutableLinuxX64
```

产物位于 `terminalApp/build/bin/linuxX64/releaseExecutable/eimusic.kexe`。

macOS（Apple Silicon）：

```bash
./gradlew :terminalApp:linkReleaseExecutableMacosArm64
```

> 只编译服务端 / TUI 时，可加 `-DincludeClient=false` 跳过客户端模块（`server/Dockerfile` 依赖这个开关）。

## 配置

- 服务端配置：`server/src/main/resources/application.conf`
- 音乐源配置：将 `eimusic.example.json` 复制为 `eimusic.json` 后填写音乐源服务地址

## 技术栈

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) / [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- Android 端媒体播放：[ExoPlayer](https://developer.android.com/media/media3/exoplayer)
- 桌面端媒体播放：[Java Stream Player](https://github.com/goxr3plus/java-stream-player)
- 服务端：[Ktor](https://ktor.io/)
- TUI：[Mosaic miniaudio](https://github.com/SparrowAndSnow/mosaic)

## License

本项目基于 [MIT](LICENSE) 协议开源。

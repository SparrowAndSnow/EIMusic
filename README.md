# EIMusic

开源音乐播放器，基于 Kotlin Multiplatform 构建。

## 模块

- `composeApp` — Compose Multiplatform 客户端，支持 Android / iOS / Desktop / Web
- `shared` — 各端共享的核心代码（数据模型、网络、认证等）
- `server` — Ktor 服务端，提供房间同步等能力
- `terminalApp` — 基于 Mosaic 的终端（TUI）客户端，支持 Linux / macOS / Windows

## 环境要求

- JDK 17+（项目使用 Gradle 9.4，wrapper 已内置）
- Android SDK（构建 Android 端时需要）
- 构建 Web 端需要 [Kotlin/Wasm](https://kotl.in/wasm/) 支持

### mosaic（编译 terminalApp 的必需依赖）

`settings.gradle.kts` 通过 `includeBuild("./mosaic")` 引入本地复合构建，且该行无条件生效——
**缺少 `mosaic` 目录会导致任何 Gradle 构建直接配置失败**。编译/运行前请先将 mosaic 克隆到项目根目录：

```bash
git clone https://github.com/SparrowAndSnow/mosaic
```

- 目录名必须保持 `mosaic`，位于项目根目录（与 `settings.gradle.kts` 同级）
- `terminalApp` 的 TUI 依赖其产物 `com.jakewharton.mosaic:mosaic-runtime:0.19.0-SNAPSHOT`

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
./gradlew :composeApp:assembleDebug
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

> 只编译服务端 / TUI 时，可加 `-DincludeClient=false` 跳过 `composeApp`，显著缩短构建时间。

## 配置

- 服务端配置：`server/src/main/resources/application.conf`
- 音乐源配置：将 `eimusic.example.json` 复制为 `eimusic.json` 后填写音乐源服务地址

## 技术栈

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) / [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- Android 端媒体播放：[ExoPlayer](https://developer.android.com/media/media3/exoplayer)
- 桌面端媒体播放：[Java Stream Player](https://github.com/goxr3plus/java-stream-player)（本地 jar：`composeApp/libs/java-stream-player-10.0.0.jar`）
- 服务端：[Ktor](https://ktor.io/)
- TUI：[Mosaic](https://github.com/SparrowAndSnow/mosaic)

## License

本项目基于 [MIT](LICENSE) 协议开源。

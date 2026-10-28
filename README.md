# EIMusic

开源音乐播放器，基于 Kotlin Multiplatform 构建。

## 模块

- `composeApp` — Compose Multiplatform 共享客户端，支持 Android / iOS / Desktop / Web
- `androidApp` — Android 应用入口，依赖 `composeApp`
- `shared` — 各端共享的核心代码（数据模型、网络、认证等）
- `server` — Ktor 服务端，提供房间同步等能力
- `terminalApp` — 基于 Mosaic 的终端（TUI）客户端，支持 Linux / macOS / Windows

## 环境要求

- JDK 17+
- Android SDK（构建 Android 端时需要）
- 构建 Web 端需要 [Kotlin/Wasm](https://kotl.in/wasm/) 支持

### mosaic（编译 terminalApp 的必需依赖）

`settings.gradle.kts` 通过 `includeBuild(...)` 引入本地复合构建，且无条件生效——
**找不到 `mosaic` 目录会导致任何 Gradle 构建直接配置失败**（项目根目录或相邻目录均可）：

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

Windows：

```bash
./gradlew :terminalApp:linkReleaseExecutableMingwX64
```

## 配置

- 服务端配置：`server/src/main/resources/application.conf`
- 音乐源配置：将 `eimusic.example.json` 复制为 `eimusic.json` 后填写音乐源服务地址

## 版本检查

客户端直接请求 GitHub Releases API（`SparrowAndSnow/EIMusic`）并对比 `tag_name`，不再依赖服务端 `/version/check`。服务端该接口仅保留给旧客户端兼容。仓库必须保持公开，否则匿名 API 会返回 404。

## CI/CD

仓库已配置 GitHub Actions：

- `.github/workflows/ci.yml`：PR/主分支构建 shared、server、desktop、Android debug APK，并编译 terminal。
- `.github/workflows/release.yml`：推送 `v*` 标签时校验 `version.toml`，构建 Android APK、server fat jar、Linux desktop zip，发布 GitHub Release，并推送 `ghcr.io/<owner>/eimusic-backend` 镜像。

发布前先把 `version.toml` 的 `version.name` 改成与标签一致，例如：

```toml
[version]
name = "1.0.0"
code = 100
```

```bash
git tag v1.0.0
git push origin v1.0.0
```

CI 会克隆 `SparrowAndSnow/mosaic` 到项目根目录作为 terminalApp 的复合构建依赖；本地代理配置请放在 `~/.gradle/gradle.properties`，不要提交到仓库。

## 技术栈

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) / [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- Android 端媒体播放：[ExoPlayer](https://developer.android.com/media/media3/exoplayer)
- 桌面端媒体播放：[Java Stream Player](https://github.com/goxr3plus/java-stream-player)
- 服务端：[Ktor](https://ktor.io/)
- TUI：[Mosaic miniaudio](https://github.com/SparrowAndSnow/mosaic)

## License

本项目基于 [MIT](LICENSE) 协议开源。

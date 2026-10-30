# EIMusic

开源音乐播放器，基于 Kotlin Multiplatform / Compose Multiplatform 构建。

## 平台支持

| 平台/组件 | 状态 | 说明 |
| --- | --- | --- |
| Android | ✅ | `:androidApp:assembleDebug` 通过；CI 构建 debug APK，Release 使用签名 release APK |
| iOS | 🚧 | 代码/目标已接入，未在本机编译验证 |
| Desktop | ✅ | 开发用目标；不发布 Desktop 发行包 |
| Web（wasmJs） | ❌ | 当前编译失败，暂不可用 |
| Terminal Linux | ✅ | release 二进制已构建 |
| Terminal Windows | ✅ | release workflow 构建 |
| Terminal macOS | 🚧 | 构建目标存在；未加入 Release |
| Server | ✅ | fat jar 已构建；暂无测试 |

> 状态为当前仓库实测结果；“✅”不代表已经发布。

## 模块

- `composeApp` — Compose Multiplatform 共享客户端
- `androidApp` — Android 应用入口
- `shared` — 共享核心代码（模型、网络、认证、SQLDelight 仓储）
- `server` — Ktor 服务端（认证、房间同步、网易云接口代理）
- `terminalApp` — 基于 Mosaic 的终端（TUI）客户端

## 技术栈

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) / [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- Android 媒体播放：[ExoPlayer](https://developer.android.com/media/media3/exoplayer)
- 桌面媒体播放：[Java Stream Player](https://github.com/goxr3plus/java-stream-player)
- 服务端：[Ktor](https://ktor.io/)
- 本地存储：[SQLDelight](https://sqldelight.github.io/sqldelight/)
- TUI：[Mosaic miniaudio](https://github.com/SparrowAndSnow/mosaic)

## 构建须知

- JDK 25（`server` 使用 `jvmToolchain(25)`，CI 也使用 JDK 25；Gradle 可通过 foojay resolver 自动下载）
- Android SDK platform 37（构建 Android 端时需要）
- terminalApp 依赖本地 `mosaic` 复合构建，`settings.gradle.kts` 会无条件 `includeBuild`：

```bash
git clone https://github.com/SparrowAndSnow/mosaic
```

- Web（wasmJs）当前不可用。
- Release 的 Android 签名依赖仓库 Secrets：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`；未配置时 release workflow 会直接失败。
  - `ANDROID_KEYSTORE_BASE64` 可用 `base64 -w0 release.keystore` 生成；keystore 生成后请离线备份，不要提交到仓库。

## 构建与运行

### 服务端

```bash
./gradlew :server:run
```

### 桌面客户端

```bash
./gradlew :composeApp:run
```

Desktop 仅用于开发运行，不再发布 Desktop 发行包；桌面用户使用 terminalApp。

### Android

```bash
./gradlew :androidApp:assembleDebug
```

### 终端客户端

Linux：

```bash
./gradlew :terminalApp:linkReleaseExecutableLinuxX64
```

macOS（Apple Silicon）：

```bash
./gradlew :terminalApp:linkReleaseExecutableMacosArm64
```

Windows：

```bash
./gradlew :terminalApp:linkReleaseExecutableMingwX64
```

产物位于 `terminalApp/build/bin/<target>/releaseExecutable/`。

### Web

Web 开发中：

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

## 配置

- 服务端：`server/src/main/resources/application.conf`
- 服务端音乐源/地址：首次运行会在工作目录生成 `eimusic.json`，可直接编辑
- 配置目录（存放 `settings.json` / `eimusic.json` / `eimusic.db`）：
  - Linux：`~/.config/eimusic/`
  - macOS：`~/Library/Application Support/eimusic/`
  - Windows：`%APPDATA%\eimusic\`

## CI / Release

- CI：push `main` / PR 构建 shared、server、desktop，构建 Android debug APK，并编译 terminal Linux。
- Release：推送 `v<version.toml 的 version.name>` tag 触发；构建签名 release APK、server fat jar、Linux/Windows terminal，发布 GitHub Release 并推送 GHCR 镜像。
- 更新检查：客户端直接读取 GitHub Releases 最新版本；服务端不再提供版本检查接口。

## 已知限制

- Web（wasmJs）当前不可用。
- 已发布 [v0.1.0 测试 Release](https://github.com/SparrowAndSnow/EIMusic/releases/tag/v0.1.0)；客户端版本检查可直接读取 GitHub Releases。
- macOS terminal 暂未加入 Release 构建；macOS 更新检查回退到 Release 页面。
- server 没有测试；composeApp/terminalApp 缺少平台级测试（shared 已有 JVM 测试）。
- terminalApp 更新检查只显示下载地址，不自动替换二进制。

## License

本项目基于 [MIT](LICENSE) 协议开源。

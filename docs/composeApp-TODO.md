# composeApp 跟进（TODO）

> 位置：`docs/composeApp-TODO.md` · 维护规矩见 [`PROJECT_RULES.md`](PROJECT_RULES.md)
> 持久化改造跟踪。terminalApp 已先迁移到 SQLDelight；composeApp 已完成歌单、最近播放/搜索历史的数据接入和会话恢复，`Settings` / `Auth` / `AppConfig` 仍使用 JSON。
> 更新：2026-10-29

## 背景

- `shared` 已包含 SQLDelight schema 与仓储：
  - `EIMusicDatabase`
  - `DbPlaylistRepository`（本地/导入歌单、只读导入、混合来源曲目）
  - `DbLibraryRepository`（最近播放、搜索历史）
  - `DbSessionRepository`（lastTab、队列、当前曲目，不记录位置）
- terminalApp 使用 native driver，DB 位于平台配置目录的 `eimusic.db`。
- composeApp 已接入：
  - Android：`AndroidSqliteDriver`（构建通过，待设备验证）
  - Desktop/JVM：`JdbcSqliteDriver`，DB 位于 `~/.config/eimusic/eimusic.db`
  - iOS：`NativeSqliteDriver`（代码已加，待 macOS 编译验证）
  - wasmJs：当前不可用。编译缺少 `NotificationService` / `log` 的 actual；且 `createEIMusicDatabase()` 是 eager 创建并直接 `error()`，JSON fallback 未接
- shared schema 迁移：`1.sqm`（recent/search_history）、`2.sqm`（session/queue_item）。

## 已完成

- [x] **Playlist 迁移**
  - `MyViewModel` → `PlaylistStore` → `DbPlaylistRepository`，旧 `Settings.playlists` 一次性导入
  - 导入歌单（网易等）只读
  - 本地歌单 UI（创建/加曲/删除/排序）已完成（`My.kt`、`PlaylistDetailPane.kt`）
  - 仓储支持混合来源曲目
- [x] **会话恢复**
  - `PlayerViewModel`/`PlaybackStore` 保存队列和当前曲目（不记录位置）
  - `App.kt` 用 `DbSessionRepository.load().lastTab` 恢复上次页面；`PlaybackStore` 恢复队列/当前曲目
- [x] **旧数据迁移**
  - `Settings.playlists`、`Settings.recentTracks`、`Settings.recentSearches` 均已接入一次性迁移

## 进行中

- [~] **Driver 接入**
  - [x] Desktop/JVM：`JdbcSqliteDriver`，建库路径 `~/.config/eimusic/eimusic.db`
  - [x] Android：`AndroidSqliteDriver`，构建通过，待设备验证
  - [ ] iOS：`NativeSqliteDriver`，待 macOS 编译验证
  - [ ] wasmJs：待修复（见下）
  - [x] schema 版本/migration 与 terminalApp 共用
  - 建库路径：Desktop 与 terminalApp 一致；Android 为应用私有目录，iOS 为 `NSHomeDirectory()/.config/eimusic`
- [~] **最近播放 / 搜索历史**
  - [x] `PlaybackStore` 记录最近播放，`SearchStore` 记录搜索词
  - [x] 搜索历史 UI 已在 TopBar 显示
  - [ ] 最近播放 UI 仍待做（composeApp 目前没有 `recentTracks` 展示）
- [~] **UI 对齐**
  - [x] 本地歌单创建、添加曲目、删除/排序
  - [ ] 曲目右键菜单：Add to playlist / Play next / Track info（当前只有 kebab `DropdownMenu`，与 terminalApp 的 TrackActionHost 语义尚未完全对齐）
  - [ ] 窄屏/响应式等 UI 差异后置

## 待做

- [ ] **wasmJs 修复**
  - 补 `NotificationService` / `log` 的 wasm actual（no-op）
  - 将 `createEIMusicDatabase()` 改为惰性/内存 fallback，避免 `createdAtStart` 直接抛错
  - `Storage.wasmJs` 持久化，或明确“内存态、不持久化”的语义
  - 在 CI 增加 `:composeApp:compileKotlinWasmJs`
- [ ] **composeApp 测试**
  - JVM 覆盖 repository CRUD / 导入只读 / 迁移（shared 已有 18 个 JVM 测试）
  - 各平台建库/迁移 smoke test

## 注意

- 不要提交本地 `gradle.properties` / `settings.gradle.kts` 的环境改动。
- 改动 shared schema 时同步更新 terminalApp 与 composeApp 两个 TODO。
- 数据库迁移只前进不回滚；迁移文件编号与 schema 版本保持一致。

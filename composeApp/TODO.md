# composeApp TODO

> 持久化改造跟踪。terminalApp 已先迁移到 SQLDelight；composeApp 后续需要对齐同一套 shared 仓储/schema。
> 更新：2026-10-24

## 背景

- `shared` 已包含 SQLDelight schema 与仓储：
  - `EIMusicDatabase`
  - `DbPlaylistRepository`（本地/导入歌单、只读导入、混合来源曲目）
  - `DbLibraryRepository`（最近播放、搜索历史）
  - `DbSessionRepository`（lastTab、队列、当前曲目，不记录位置）
- terminalApp 已接入 native driver，DB 位于 `defaultConfigDir()/eimusic.db`。
- composeApp 仍主要使用 `IStorage` + JSON（`Settings.playlists`、最近播放/搜索等）。
- shared schema 迁移：`1.sqm`（recent/search_history）、`2.sqm`（session/queue_item）。

## 待做

- [x] **Driver 接入（desktop 已编译验证）**
  - Android：`AndroidSqliteDriver`（代码已加，待设备验证）
  - Desktop/JVM：`JdbcSqliteDriver`，DB 位于 `~/.config/eimusic/eimusic.db`
  - iOS：`NativeSqliteDriver`（代码已加，待编译验证）
  - wasmJs：无官方 SQLite driver，暂时抛错，JSON fallback 未接
  - 建库路径与 terminalApp 保持一致，schema 版本/migration 共用

- [~] **Playlist 迁移**
  - `MyViewModel` 已改用 `DbPlaylistRepository`，旧 `Settings.playlists` 一次性导入
  - 导入歌单（网易等）只读
  - 本地歌单 UI（创建/加曲/排序）仍待做
  - 本地歌单可混合加入各来源曲目（仓储已支持）

- [~] **最近播放 / 搜索历史**
  - `PlayerViewModel` 已记录最近播放；`SearchResultViewModel` 已记录搜索词
  - UI 历史展示仍待做
  - 旧 `Settings.recentTracks` / `recentSearches` 迁移未做

- [~] **会话恢复**
  - `PlayerViewModel` 已保存队列和当前曲目（不记录位置）
  - 启动恢复和 lastTab 仍待做

- [ ] **UI 对齐**
  - 本地歌单创建、添加曲目、删除/排序
  - 曲目右键菜单：Add to playlist / Play next / Track info（与 terminalApp 的 TrackActionHost 语义一致）
  - 窄屏/响应式等 UI 差异后置

- [ ] **测试**
  - JVM 覆盖 repository CRUD / 导入只读 / 迁移
  - 各平台建库/迁移 smoke test

## 注意

- 不要提交本地 `gradle.properties` / `settings.gradle.kts` 的环境改动。
- 改动 shared schema 时同步更新 terminalApp 与 composeApp 两个 TODO。
- 数据库迁移只前进不回滚；迁移文件编号与 schema 版本保持一致。

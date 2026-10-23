# EIMusic TODO

> 更新：2026-10-23 · 范围：terminalApp（TUI）
> 状态标记：`[ ]` 未开始 · `[~]` 进行中 · `[x]` 完成
> 约定：每项先 debug + pty 验证，最后重建 release 一次；不改变现有键位语义。

## 已完成（2026-10）

- [x] terminalApp 适配重构后的 mosaic（画布/约束、BasicTextField、鼠标、备用屏）
- [x] 列表单击选中 / 双击打开、hover、滚轮惯性渐停
- [x] 底部页面 tabs（1-5 / Tab / 点击切换）
- [x] Search / Login / Help 改为库版 OverlayHost + Dialog（模态输入屏障）
- [x] Local/Playlist/Queue/Together 的键位回调收口为 Actions 对象

## P0 / 交互体验

- [x] **Header 与播放控制可点击**
  - HeaderBar / PlayerControls 从整行 AnnotatedString 改成 Row 分段
  - 已支持：播放/暂停、上一首/下一首（Main 控件）、模式、音量静音、队列、主题、登录、Together
  - 每段 clickable + hover 高亮；Header 未加 prev/next
  - 保持键盘快捷键不变
- [x] **通用曲目右键菜单**
  - `ContextMenu` + `TrackActionHost` 全局 overlay 渲染
  - Search/Queue/Playlist/Local 的曲目右键：添加到歌单、下一首播放、歌曲信息、列表额外操作
  - `ListView`/`TrackTable` 支持 onItemSecondaryClick
- [~] **进度条点击/拖动 seek**（已实现，待真机播放验证）
  - [x] ProgressBar 拆成 bar + 时间；bar 按列映射时间，Press/Drag 调用 seek
  - [-] hover 目标时间 / buffered 进度（决定暂不做）

## P1 / 功能

- [x] **搜索历史 + 最近播放**
  - [x] SearchViewModel 持久化最近查询；空查询显示历史，点击可直接搜索
  - [x] PlayerViewModel 记录最近播放（最近 12 首），Main 空状态 ListView 展示，回车/点击播放
- [~] **队列增强**
  - [x] 上移/下移（K/J）、播放已存在曲目不再重排、移动/删除后选择跟随
  - [x] `g` 跳到当前播放
  - [ ] 队列另存为歌单
- [x] **歌单编辑（仅本地歌单）**
  - [x] 导入歌单只读，本地歌单可编辑
  - [x] 从 Search/Queue 通过通用 PlaylistPicker 加入本地歌单
  - [x] 歌单内 `d` 删除、`K/J` 排序；`r` 刷新导入歌单
  - [x] 本地歌单可混合加入网易/Spotify 等来源的曲目
- [x] **会话恢复**
  - [x] 重启恢复上次 tab、队列、当前曲目（暂停，不恢复位置）；首次播放时才打开音频/网络流

## P1 / 布局与图标

- [ ] **窄终端响应式布局**
  - 断点按列宽（如 <80 / <100）切换紧凑模式
  - Header 按优先级隐藏次要段（音量百分比、Together、登录名），保留播放/队列
  - TabRow 窄屏只显示编号 + 短名，hint 截断
  - TrackTable 列优先级：名称优先，窄屏隐藏艺术家/时长；列宽按显示宽度算
  - Dialog 宽度 clamp 到终端宽度，内容超高时滚动或截断
  - Main 高度不足时缩小/隐藏专辑图，控制条与进度条始终可见
- [~] **图标体系统一**（单宽/集中已完成，ASCII 回退待做）
  - [x] `Icon.Together` 从 emoji 👥（双宽）换成 Nerd Font 单宽 `\uF0C0`
  - [x] 所有图标集中在 `ui/Icon.kt`，统一用 Font Awesome 单宽码位
  - [ ] 无 Nerd Font / 不支持 PUA 的终端提供 ASCII 回退（配置或环境变量）
  - [ ] Header、按钮、空状态、列表标记的图标与文字基线对齐；CJK/emoji 列宽按显示宽度
- [ ] **间距与对齐规范**
  - spacer/内边距常量统一；标题、列表头、对话框边框与内容对齐
  - 长标题 marquee 或两行截断（与响应式一起做）

## P2 / 美化与增强

- [ ] **主题预设**
  - 在 dark/light 之外加 3–4 套中性命名配色，`t` 循环或设置页选择
- [ ] **歌词**
  - LRC 读取/缓存，Main 切换显示当前行；无歌词回退专辑图
  - 先确定歌词来源与缓存策略
- [x] **MPRIS / 媒体键完善**
  - LoopStatus/Shuffle 可写，Volume 设置解除静音
  - Metadata 使用 Track.duration 兜底，artUrl 本地路径补 file://
  - SetPosition 校验 trackid；Seeked 信号（>2s 跳转）
  - MPRIS 仍不提供 Quit/Raise

## 工程

- [ ] **CI**
  - push 自动跑 `:shared:jvmTest` + terminalApp release 构建
- [ ] **terminalApp 测试基建**
  - 增加 commonTest；先覆盖列表窗口、格式化、Actions 映射等纯逻辑
- [ ] **安装/打包**
  - Linux release 二进制安装脚本、README 使用说明

## 已知问题 / 待查

- [ ] **主页 PlayerControls 在空状态→播放切换后尺寸错误（kitty）**
  - 现象：Main 从空状态切到播放状态后，播放/音量控件看起来变小；鼠标悬停一次后恢复。仅在 kitty 复现，alacritty 正常。
  - 已排除：不是图标字形（同一套 Icon 在 Header 正常）；hover 前后控件 cell 内容完全一致。
  - 规避：去掉 `PlayerControls` 所在的 if/else 空状态分支，让控件常驻/不参与分支切换后正常。
  - 怀疑：Mosaic 在 if/else 分支替换时布局/测量缓存未正确失效，控件复用了旧的测量结果；hover 触发重绘后重新测量。
  - 下一步：在 Mosaic 做最小复现（分支前后同位置同控件），或临时用 `key(...)` 强制重建验证。

## 存储演进

- [x] **SQLDelight 接入（基础）**
  - `shared` 增加 `app.cash.sqldelight` 2.4.1，schema：track / playlist / playlist_track
  - `DbPlaylistRepository`：本地歌单 CRUD、混合来源曲目、导入歌单只读/同步替换
  - terminalApp native 使用 `NativeSqliteDriver`，写入 `defaultConfigDir()/eimusic.db`
  - JVM 测试覆盖本地顺序/移动/删除、导入只读、混合来源
- [x] **接入 UI**：PlaylistViewModel/Screen 改用 DbPlaylistRepository；URL 导入写库；旧 Settings.playlists 迁移
  - [x] `n` 新建本地歌单；`a` 导入 URL；`d` 删除记录；本地歌单持久化已验证
  - [x] Search/Queue 按 `l` 添加曲目到本地歌单
  - [x] 本地歌单内 `d` 删除、`K/J` 排序；导入歌单只读
- [x] **最近播放/搜索历史迁 DB**：recent / search_history 表 + 迁移；Settings 只留偏好
- [x] **会话恢复（DB session）**：lastTab / 队列 / 当前曲目（暂停恢复，不记录位置）

- 当前：`Settings` JSON 存偏好、最近搜索、最近播放；队列只存在内存。
- 目标：按领域拆分，避免单文件频繁整写：
  - `settings.json`：主题/音量/静音/播放模式/tab
  - `session.json`：队列、当前曲目、位置
  - `playlists.json` / `recent.json`
  - 原子写（tmp + rename + .bak）+ version 字段 + 节流落盘
- 换 SQLite：用 SQLDelight；表：track / playlist / playlist_track / recent / queue_item / session / search_history。
- 歌单模型：`playlist.source`（LOCAL / NETEASE / SPOTIFY…）即“本地歌单”标记；只有 `source=LOCAL` 可增删/排序曲目；导入歌单只读。
- 曲目模型：`track.id` 全局唯一（如 `netease:123`、`spotify:...`、`local:<hash>`），本地歌单可混合加入各来源曲目。
- 在此之前不引入数据库依赖，保持接口可替换。

## 暂不做

- 进度条 hover 目标时间 / buffered 进度
- Loading / Scanning 动画
- backdrop dim/invert（已决定保持 Solid 全擦）
- 非 modal Popup 点击外部关闭（语义未定）

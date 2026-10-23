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

- [ ] **Header 与播放控制可点击**
  - HeaderBar / PlayerControls 从整行 AnnotatedString 改成 Row 分段
  - 播放/暂停、上一首/下一首、模式、音量、队列、主题、登录、Together
  - 每段 clickable + hover 高亮；保持键盘快捷键不变
- [ ] **进度条点击/拖动 seek**
  - ProgressBar 按列映射时间；点击 seek，拖动持续 seek
  - hover 显示目标时间；增加 buffered 进度
- [ ] **加载 / 扫描动画**
  - `Loading...`、`Scanning...` 改为 spinner 或扫描计数
  - 长操作不再看起来像卡死

## P1 / 功能

- [ ] **搜索历史 + 最近播放**
  - SearchViewModel 持久化最近查询；空查询显示历史
  - Main 空状态显示最近播放，回车直接播放
- [ ] **队列增强**
  - 上移/下移（controller 已有 moveTrack）
  - `g` 跳到当前播放；队列另存为歌单
- [ ] **收藏 / 喜欢**
  - 本地收藏 track，列表标记，过滤“只看收藏”
  - 需要 storage schema 扩展与迁移
- [ ] **歌单编辑**
  - 从 Search/Queue 把选中曲目加入歌单
  - 歌单内删除/排序；当前只能加/删整个歌单
- [ ] **会话恢复**
  - 重启恢复上次 tab、队列、音量、播放位置、主题

## P2 / 美化与增强

- [ ] **主题预设**
  - 在 dark/light 之外加 3–4 套中性命名配色，`t` 循环或设置页选择
- [ ] **长标题 / 响应式布局**
  - 长标题 marquee 或两行截断；窄终端列宽自适应
- [ ] **歌词**
  - LRC 读取/缓存，Main 切换显示当前行；无歌词回退专辑图
  - 先确定歌词来源与缓存策略
- [ ] **MPRIS / 媒体键完善**
  - 检查 seek、metadata、播放模式是否都暴露

## 工程

- [ ] **CI**
  - push 自动跑 `:shared:jvmTest` + terminalApp release 构建
- [ ] **terminalApp 测试基建**
  - 增加 commonTest；先覆盖列表窗口、格式化、Actions 映射等纯逻辑
- [ ] **安装/打包**
  - Linux release 二进制安装脚本、README 使用说明

## 暂不做

- backdrop dim/invert（已决定保持 Solid 全擦）
- 非 modal Popup 点击外部关闭（语义未定）

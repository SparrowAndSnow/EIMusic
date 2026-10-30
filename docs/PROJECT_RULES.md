# EIMusic 项目跟进与文档维护规矩

> 目的：保证 README、跟进文档、审查记录与代码、构建、Release 状态一致；信息不缺失、不重复、不漂移。
> 适用范围：所有功能、修复、构建/CI/发布变更。
> 最后核对：2026-10-30

## 1. 信息分层（单一来源）

| 文档 | 只放什么 | 不放什么 |
| --- | --- | --- |
| `README.md` | 项目定位、平台支持表、模块、技术栈、构建须知、构建/运行命令、配置入口、Release 概览、已知限制 | 逐项功能状态、键位表、设置页细节、长篇排查记录 |
| `docs/terminalApp-TODO.md` | terminalApp 的功能、交互、键位、设置、MPRIS、测试、已知问题 | composeApp 持久化细节、历史审查全文 |
| `docs/composeApp-TODO.md` | composeApp 的持久化、DB、UI、测试、待做 | terminalApp 细节 |
| `docs/audits/YYYY-MM-DD-*.md` | 某次审查的快照与证据 | 持续更新的 TODO（审查记录不改写，只追加新日期） |
| `docs/README.md` | 文档索引、最后核对日期 | 项目介绍、构建命令 |

规则：同一事实只在一个地方维护；其他地方用链接引用，不复制状态。

## 2. 状态标记

- `[ ]` 未开始
- `[~]` 进行中/部分完成：必须写清“已完成什么、还差什么、下一步”
- `[x]` 已完成：必须有验证证据（命令/CI/平台/日期/commit），不能只凭“代码已写”
- `[-]` 明确不做：写原因
- 不用“应该/大概/可能”描述状态；不确定就标 `[~]`，并写“未验证”。

## 3. 每条跟进项的最小信息

每个 `[ ]` / `[~]` 项至少包含：

1. 范围/验收标准（做什么、做到什么算完成）
2. 当前状态（已完成/未完成/阻塞）
3. 验证方式（命令、平台、CI job、手工步骤）
4. 下一步（具体动作）
5. 相关文件/链接（如 `MainScreen.kt:69`）
6. 日期（最后核对/更新）

已完成项移动到“已完成”区，保留简短证据；不要删除历史。

## 4. 更新触发条件（强制）

以下变更必须在同一次提交/PR 中更新文档：

- 功能/交互/键位/设置项变化 → `docs/terminalApp-TODO.md` 或 `docs/composeApp-TODO.md`
- 平台支持/构建命令/依赖版本变化 → `README.md` 的平台表/构建须知
- CI/Release 流程变化 → `README.md` 的 CI 段 + 对应 TODO 的 CI 项
- 修复 bug → 标 `[x]` 并写验证证据；未验证则标 `[~]`
- 新增已知限制 → 写入对应 TODO；用户可见的限制同步到 README“已知限制”
- 版本/发布 → `version.toml`、tag、GitHub Release、更新检查、README 状态
- 文档迁移/重命名 → 更新 `docs/README.md` 索引和所有引用链接

## 5. 平台状态口径

README 平台表：

- ✅ 已验证：本机/CI 至少一次真实构建或运行通过；注明验证方式
- 🚧 部分/未验证：代码或目标存在，但未编译/未运行/未上设备
- ❌ 不可用：当前构建失败或功能缺失

“✅”不代表已发布；发布状态单独说明。

每个平台结论必须能追溯到：

- 本地命令输出，或
- GitHub Actions run，或
- 明确标注“未验证”。

## 6. 发布前检查清单

- [ ] `version.toml` 的 `version.name` 与 tag 一致
- [ ] `:shared:jvmTest` 通过
- [ ] `:server:buildFatJar` 通过
- [ ] `:composeApp:compileKotlinDesktop` 通过
- [ ] `:androidApp:assembleDebug` 通过
- [ ] Android release 签名 secrets 已配置（`ANDROID_KEYSTORE_BASE64` / `ANDROID_KEYSTORE_PASSWORD` / `ANDROID_KEY_ALIAS` / `ANDROID_KEY_PASSWORD`）
- [ ] `:androidApp:assembleRelease` 产出可安装的签名 APK
- [ ] `:terminalApp:compileKotlinLinuxX64` 通过
- [ ] 已知 wasm 状态已记录（当前不可用）
- [ ] GitHub Release 资产存在（Android APK / Linux / Windows）
- [ ] 更新检查能读到新版本
- [ ] README 平台表/CI 段与 release workflow 一致
- [ ] TODO 中已完成项有验证证据
- [ ] `docs/README.md` 的“最后核对”日期更新

## 7. 定期审查

- 每个 release 前做一次文档审查；至少每月一次。
- 审查产物放 `docs/audits/YYYY-MM-DD-<topic>.md`，只追加，不改写历史。
- 审查范围：README 平台表/构建/CI/Release、TODO 状态、代码实际、CI 实际、GitHub Release 实际。
- 审查后更新 `docs/README.md` 索引和日期。

## 8. README 约束

- README 只保留“现在的大体框架”：平台支持、模块、技术栈、构建须知、构建/运行、配置入口、CI/Release 概览、已知限制。
- 不把键位表、设置页细节、逐项功能清单、排查过程塞进 README；这些放对应 TODO 或独立文档。
- README 不复制 TODO 状态；需要时链接到 docs。

## 9. 文件命名

- `docs/terminalApp-TODO.md`
- `docs/composeApp-TODO.md`
- `docs/PROJECT_RULES.md`
- `docs/README.md`
- `docs/audits/YYYY-MM-DD-<topic>.md`
- 新增文档必须登记到 `docs/README.md`。

## 10. 信息不缺失的兜底

- 任何口头讨论出的待做/限制/风险，必须落到对应 TODO 或 audit，才算“记录”。
- 不确定是否完成 → `[~]`，不标 `[x]`。
- 跨平台差异必须写平台；不能用一个平台的结果代表所有平台。
- 过时内容不删除，移到“历史/已完成”并注明日期；删除前先确认已被新文档覆盖。

# 管理端跟进

> 最后核对：2026-10-30

## 已落地

- Ktor 服务端渲染管理端，入口 `/admin`
- 首次访问初始化管理员：`/admin/login` 在没有管理员时显示初始化表单，账号和密码哈希写入 `users` 表，不依赖管理员环境变量
- 管理员登录使用数据库密码，PBKDF2-SHA256 哈希
- 音源管理：按 `TrackSource` 编辑 URL 列表，保存后写入 `source_urls`，NETEASE 运行中立即切换
- 用户管理：查看 `users` 表、创建用户/管理员、重置密码、启用/禁用
- 服务状态：会话数、频道数等运行时统计
- 操作审计：`admin_audit` 表，记录管理员初始化、登录、退出、音源修改、用户创建、密码重置、启用/禁用
- CSRF 校验；音源 URL 仅允许 http/https，并拒绝内网/环回地址
- 管理端集成测试：首次初始化、登录、CSRF、音源修改、审计写入

## 待办

- 登录失败限流 / 锁定
- 审计分页、导出、按时间筛选
- 服务状态页自动刷新（HTMX 轮询或 SSE）
- 生产环境强制 HTTPS；可选设置 `EIMUSIC_ADMIN_SECURE_COOKIE=true` 和固定 `EIMUSIC_ADMIN_SESSION_SECRET`
- 数据库版本化迁移（当前用 `SchemaUtils.createMissingTablesAndColumns` 兼容旧库）

## 验证

```bash
./gradlew :server:test
```

手动检查：

```text
GET  /admin          -> 未登录跳转 /admin/login
POST /admin/login    -> 正确密码后进入仪表盘
GET  /admin/sources  -> 编辑并保存音源
GET  /admin/users    -> 创建用户
GET  /admin/status   -> 查看运行时统计
GET  /admin/audit    -> 查看审计记录
```

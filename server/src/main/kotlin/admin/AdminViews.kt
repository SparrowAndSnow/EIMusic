package com.eimsound.eimusic.server.admin

import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.server.db.AuditRecord
import com.eimsound.eimusic.server.db.UserRecord
import kotlinx.html.*

private fun HTML.adminLayout(
    title: String,
    session: AdminSession?,
    message: String?,
    content: FlowContent.() -> Unit,
) {
    head {
        meta(charset = "utf-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1")
        title { +"$title · EIMusic Admin" }
        link {
            rel = "stylesheet"
            href = "/admin/static/admin.css"
        }
    }
    body {
        header {
            div("header-inner") {
                strong("brand") { +"EIMusic Admin" }
                if (session?.authenticated == true) {
                    nav {
                        a(href = "/admin") { +"仪表盘" }
                        a(href = "/admin/sources") { +"音源" }
                        a(href = "/admin/users") { +"用户" }
                        a(href = "/admin/status") { +"状态" }
                        a(href = "/admin/audit") { +"审计" }
                        a(href = "/admin/logout", classes = "logout") { +"退出" }
                    }
                }
            }
        }
        main("container") {
            if (!message.isNullOrBlank()) {
                div("flash") { +message }
            }
            content()
        }
    }
}

fun HTML.adminLoginPage(error: String? = null) {
    adminLayout("登录", null, null) {
        section("login-card") {
            h1 { +"管理员登录" }
            if (!error.isNullOrBlank()) {
                div("error") { +error }
            }
            form(action = "/admin/login", method = FormMethod.post) {
                label {
                    +"用户名"
                    input {
                        type = InputType.text
                        name = "username"
                        required = true
                    }
                }
                label {
                    +"密码"
                    input {
                        type = InputType.password
                        name = "password"
                        required = true
                    }
                }
                button {
                    type = ButtonType.submit
                    +"登录"
                }
            }
        }
    }
}

fun HTML.adminSetupPage(error: String? = null) {
    adminLayout("初始化", null, null) {
        section("login-card") {
            h1 { +"初始化管理员" }
            p("muted") {
                +"首次使用管理端，请创建第一个管理员账号。账号和密码哈希会写入 users 表。"
            }
            if (!error.isNullOrBlank()) {
                div("error") { +error }
            }
            form(action = "/admin/setup", method = FormMethod.post) {
                label {
                    +"用户名"
                    input {
                        type = InputType.text
                        name = "username"
                        value = "admin"
                        required = true
                    }
                }
                label {
                    +"密码（至少 8 位）"
                    input {
                        type = InputType.password
                        name = "password"
                        required = true
                    }
                }
                label {
                    +"确认密码"
                    input {
                        type = InputType.password
                        name = "confirm"
                        required = true
                    }
                }
                button {
                    type = ButtonType.submit
                    +"创建管理员并登录"
                }
            }
        }
    }
}

fun HTML.adminDashboardPage(
    session: AdminSession,
    message: String?,
    sessionStats: Map<String, Any>,
    channelStats: Map<String, Any>,
    userCount: Int,
    sourceCount: Int,
) {
    adminLayout("仪表盘", session, message) {
        h1 { +"仪表盘" }
        div("grid") {
            statCard("在线会话", sessionStats["totalSessions"] ?: 0)
            statCard("活跃会话", sessionStats["activeSessions"] ?: 0)
            statCard("频道数", channelStats["totalChannels"] ?: 0)
            statCard("频道在线用户", channelStats["onlineUsers"] ?: 0)
            statCard("用户数", userCount)
            statCard("音源数", sourceCount)
        }
        p("muted") {
            +"管理端只做服务端配置和运维，不直接暴露 SQLite 文件。"
        }
    }
}

fun HTML.adminSourcesPage(
    session: AdminSession,
    message: String?,
    sources: Map<TrackSource, List<String>>,
) {
    adminLayout("音源", session, message) {
        h1 { +"音源管理" }
        p("muted") {
            +"每行一个 URL，按顺序用于回退；只允许 http/https，禁止内网地址。"
        }
        TrackSource.entries.forEach { source ->
            val urls = sources[source].orEmpty()
            section("card") {
                h2 { +source.name }
                form(action = "/admin/sources", method = FormMethod.post) {
                    input {
                        type = InputType.hidden
                        name = "csrf"
                        value = session.csrf
                    }
                    input {
                        type = InputType.hidden
                        name = "source"
                        value = source.name
                    }
                    textArea {
                        name = "urls"
                        rows = "8"
                        +urls.joinToString("\n")
                    }
                    button {
                        type = ButtonType.submit
                        +"保存 ${source.name}"
                    }
                }
            }
        }
    }
}

fun HTML.adminUsersPage(
    session: AdminSession,
    message: String?,
    users: List<UserRecord>,
) {
    adminLayout("用户", session, message) {
        h1 { +"用户管理" }
        section("card") {
            h2 { +"新建用户" }
            form(action = "/admin/users", method = FormMethod.post) {
                input {
                    type = InputType.hidden
                    name = "csrf"
                    value = session.csrf
                }
                label {
                    +"用户名"
                    input {
                        type = InputType.text
                        name = "username"
                        required = true
                    }
                }
                label {
                    +"初始密码"
                    input {
                        type = InputType.password
                        name = "password"
                        required = true
                    }
                }
                label {
                    +"角色"
                    select {
                        name = "role"
                        option {
                            value = AdminAccountService.ROLE_USER
                            +"普通用户"
                        }
                        option {
                            value = AdminAccountService.ROLE_ADMIN
                            +"管理员"
                        }
                    }
                }
                button {
                    type = ButtonType.submit
                    +"创建"
                }
            }
        }
        section("card") {
            h2 { +"用户列表" }
            if (users.isEmpty()) {
                p { +"暂无用户。" }
            } else {
                table {
                    thead {
                        tr {
                            th { +"ID" }
                            th { +"用户名" }
                            th { +"角色" }
                            th { +"状态" }
                            th { +"创建时间" }
                            th { +"操作" }
                        }
                    }
                    tbody {
                        users.forEach { user ->
                            tr {
                                td { +user.id.toString() }
                                td { +user.username }
                                td { +user.role }
                                td {
                                    if (user.disabled) {
                                        +"已禁用"
                                    } else {
                                        +"正常"
                                    }
                                }
                                td { +formatTimestamp(user.createdAt) }
                                td {
                                    div("row-actions") {
                                        form(action = "/admin/users/${user.id}/password", method = FormMethod.post) {
                                            input {
                                                type = InputType.hidden
                                                name = "csrf"
                                                value = session.csrf
                                            }
                                            input {
                                                type = InputType.password
                                                name = "password"
                                                placeholder = "新密码"
                                                required = true
                                            }
                                            button {
                                                type = ButtonType.submit
                                                +"重置密码"
                                            }
                                        }
                                        form(action = "/admin/users/${user.id}/toggle", method = FormMethod.post) {
                                            input {
                                                type = InputType.hidden
                                                name = "csrf"
                                                value = session.csrf
                                            }
                                            button {
                                                type = ButtonType.submit
                                                +if (user.disabled) "启用" else "禁用"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun HTML.adminStatusPage(
    session: AdminSession,
    message: String?,
    sessionStats: Map<String, Any>,
    channelStats: Map<String, Any>,
) {
    adminLayout("状态", session, message) {
        h1 { +"服务状态" }
        section("card") {
            h2 { +"会话" }
            statsTable(sessionStats)
        }
        section("card") {
            h2 { +"频道" }
            statsTable(channelStats)
        }
    }
}

fun HTML.adminAuditPage(
    session: AdminSession,
    message: String?,
    records: List<AuditRecord>,
) {
    adminLayout("审计", session, message) {
        h1 { +"操作审计" }
        section("card") {
            if (records.isEmpty()) {
                p { +"暂无审计记录。" }
            } else {
                table {
                    thead {
                        tr {
                            th { +"时间" }
                            th { +"操作者" }
                            th { +"动作" }
                            th { +"详情" }
                        }
                    }
                    tbody {
                        records.forEach { record ->
                            tr {
                                td { +formatTimestamp(record.createdAt) }
                                td { +record.actor }
                                td { +record.action }
                                td { +record.detail }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun FlowContent.statCard(label: String, value: Any) {
    div("card stat") {
        div("stat-value") { +value.toString() }
        div("stat-label") { +label }
    }
}

private fun FlowContent.statsTable(stats: Map<String, Any>) {
    table {
        tbody {
            stats.forEach { (key, value) ->
                tr {
                    th { +key }
                    td { +value.toString() }
                }
            }
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

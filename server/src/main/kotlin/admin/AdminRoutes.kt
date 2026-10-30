package com.eimsound.eimusic.server.admin

import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.network.RequestUrlProvider
import com.eimsound.eimusic.server.db.AuditRepository
import com.eimsound.eimusic.server.db.SourceRepository
import com.eimsound.eimusic.server.db.UserRepository
import com.eimsound.eimusic.server.manager.ChannelManager
import com.eimsound.eimusic.server.manager.UserSessionManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import java.net.URI
import java.net.URLEncoder

fun Route.adminRoutes(
    sourceRepository: SourceRepository,
    userRepository: UserRepository,
    auditRepository: AuditRepository,
    requestUrlProvider: RequestUrlProvider,
    adminAccounts: AdminAccountService,
    sessionManager: UserSessionManager,
    channelManager: ChannelManager,
) {
    staticResources("/admin/static", "admin")

    get("/admin/login") {
        if (!adminAccounts.hasAdmin()) {
            call.respondHtml {
                adminSetupPage(call.request.queryParameters["error"])
            }
        } else {
            call.respondHtml {
                adminLoginPage(call.request.queryParameters["error"])
            }
        }
    }

    post("/admin/setup") {
        if (adminAccounts.hasAdmin()) {
            call.respondRedirect("/admin/login?error=${encode("管理员账号已初始化")}")
            return@post
        }

        val params = call.receiveParameters()
        val username = params["username"].orEmpty().trim()
        val password = params["password"].orEmpty()
        val confirm = params["confirm"].orEmpty()

        if (username.isBlank() || username.length > 64) {
            call.respondRedirect("/admin/login?error=${encode("用户名不能为空且不能超过 64 个字符")}")
            return@post
        }
        if (password.length < 8) {
            call.respondRedirect("/admin/login?error=${encode("密码至少 8 个字符")}")
            return@post
        }
        if (password != confirm) {
            call.respondRedirect("/admin/login?error=${encode("两次输入的密码不一致")}")
            return@post
        }

        val user = adminAccounts.initializeFirstAdmin(username, password)
        if (user == null) {
            call.respondRedirect("/admin/login?error=${encode("管理员账号已初始化")}")
            return@post
        }

        auditRepository.record(user.username, "admin.setup", "initial admin")
        call.sessions.set(
            AdminSession(
                authenticated = true,
                username = user.username,
                csrf = java.util.UUID.randomUUID().toString(),
            )
        )
        auditRepository.record(user.username, "login", "success")
        call.respondRedirect("/admin")
    }

    post("/admin/login") {
        if (!adminAccounts.hasAdmin()) {
            call.respondRedirect("/admin/login")
            return@post
        }

        val params = call.receiveParameters()
        val username = params["username"].orEmpty()
        val password = params["password"].orEmpty()
        val user = adminAccounts.authenticate(username, password)

        if (user == null) {
            auditRepository.record(username.ifBlank { "unknown" }, "login", "failed")
            call.respondRedirect("/admin/login?error=${encode("用户名或密码错误")}")
            return@post
        }

        call.sessions.set(
            AdminSession(
                authenticated = true,
                username = user.username,
                csrf = java.util.UUID.randomUUID().toString(),
            )
        )
        auditRepository.record(user.username, "login", "success")
        call.respondRedirect("/admin")
    }

    get("/admin/logout") {
        call.sessions.get<AdminSession>()?.let { session ->
            auditRepository.record(session.username, "logout", "success")
        }
        call.sessions.clear<AdminSession>()
        call.respondRedirect("/admin/login")
    }

    get("/admin") {
        val session = call.adminSession() ?: return@get
        val sessionStats = sessionManager.getStatistics()
        val channelStats = channelManager.getSystemStats()
        val users = userRepository.all()
        val sources = sourceRepository.load()

        call.respondHtml {
            adminDashboardPage(
                session = session,
                message = call.request.queryParameters["message"],
                sessionStats = sessionStats,
                channelStats = channelStats,
                userCount = users.size,
                sourceCount = sources.size,
            )
        }
    }

    get("/admin/sources") {
        val session = call.adminSession() ?: return@get
        call.respondHtml {
            adminSourcesPage(
                session = session,
                message = call.request.queryParameters["message"],
                sources = sourceRepository.load(),
            )
        }
    }

    post("/admin/sources") {
        val session = call.adminSession() ?: return@post
        val params = call.receiveParameters()
        if (params["csrf"] != session.csrf) {
            call.respond(HttpStatusCode.Forbidden, "CSRF token invalid")
            return@post
        }

        val source = TrackSource.entries.firstOrNull { it.name == params["source"] }
        if (source == null) {
            call.redirectWithMessage("/admin/sources", "未知音源")
            return@post
        }

        val urls = params["urls"].orEmpty()
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val error = validateSourceUrls(urls)
        if (error != null) {
            call.redirectWithMessage("/admin/sources", error)
            return@post
        }

        val current = sourceRepository.load().toMutableMap()
        current[source] = urls
        if (source == TrackSource.NETEASE) {
            val updateError = runCatching { requestUrlProvider.updateUrls(urls) }.exceptionOrNull()
            if (updateError != null) {
                call.redirectWithMessage("/admin/sources", "URL 无法解析：${updateError.message}")
                return@post
            }
        }
        sourceRepository.replace(current)
        auditRepository.record(
            session.username,
            "source.update",
            "${source.name}: ${urls.joinToString(", ")}",
        )
        call.redirectWithMessage("/admin/sources", "已保存 ${source.name}")
    }

    get("/admin/users") {
        val session = call.adminSession() ?: return@get
        call.respondHtml {
            adminUsersPage(
                session = session,
                message = call.request.queryParameters["message"],
                users = userRepository.all(),
            )
        }
    }

    post("/admin/users") {
        val session = call.adminSession() ?: return@post
        val params = call.receiveParameters()
        if (params["csrf"] != session.csrf) {
            call.respond(HttpStatusCode.Forbidden, "CSRF token invalid")
            return@post
        }

        val username = params["username"].orEmpty().trim()
        if (username.isBlank() || username.length > 64) {
            call.redirectWithMessage("/admin/users", "用户名不能为空且不能超过 64 个字符")
            return@post
        }

        val password = params["password"].orEmpty()
        if (password.length < 8) {
            call.redirectWithMessage("/admin/users", "密码至少 8 个字符")
            return@post
        }

        val role = params["role"].orEmpty().ifBlank { AdminAccountService.ROLE_USER }
        if (role != AdminAccountService.ROLE_ADMIN && role != AdminAccountService.ROLE_USER) {
            call.redirectWithMessage("/admin/users", "未知角色")
            return@post
        }

        val message = try {
            userRepository.create(username, PasswordHasher.hash(password), role)
            auditRepository.record(session.username, "user.create", "$username ($role)")
            "已创建用户 $username"
        } catch (_: Exception) {
            "创建失败：用户名可能已存在"
        }
        call.redirectWithMessage("/admin/users", message)
    }

    post("/admin/users/{id}/password") {
        val session = call.adminSession() ?: return@post
        val params = call.receiveParameters()
        if (params["csrf"] != session.csrf) {
            call.respond(HttpStatusCode.Forbidden, "CSRF token invalid")
            return@post
        }

        val id = call.parameters["id"]?.toLongOrNull()
        val user = id?.let { userRepository.findById(it) }
        if (user == null) {
            call.redirectWithMessage("/admin/users", "用户不存在")
            return@post
        }

        val password = params["password"].orEmpty()
        if (password.length < 8) {
            call.redirectWithMessage("/admin/users", "密码至少 8 个字符")
            return@post
        }

        userRepository.updatePasswordHash(user.id, PasswordHasher.hash(password))
        auditRepository.record(session.username, "user.password", user.username)
        call.redirectWithMessage("/admin/users", "已重置 ${user.username} 的密码")
    }

    post("/admin/users/{id}/toggle") {
        val session = call.adminSession() ?: return@post
        val params = call.receiveParameters()
        if (params["csrf"] != session.csrf) {
            call.respond(HttpStatusCode.Forbidden, "CSRF token invalid")
            return@post
        }

        val id = call.parameters["id"]?.toLongOrNull()
        val user = id?.let { userRepository.findById(it) }
        if (user == null) {
            call.redirectWithMessage("/admin/users", "用户不存在")
            return@post
        }
        if (user.username == session.username) {
            call.redirectWithMessage("/admin/users", "不能禁用当前登录账号")
            return@post
        }

        userRepository.setDisabled(user.id, !user.disabled)
        val action = if (user.disabled) "user.enable" else "user.disable"
        auditRepository.record(session.username, action, user.username)
        val state = if (user.disabled) "启用" else "禁用"
        call.redirectWithMessage("/admin/users", "已${state} ${user.username}")
    }

    get("/admin/audit") {
        val session = call.adminSession() ?: return@get
        call.respondHtml {
            adminAuditPage(
                session = session,
                message = call.request.queryParameters["message"],
                records = auditRepository.recent(),
            )
        }
    }

    get("/admin/status") {
        val session = call.adminSession() ?: return@get
        val sessionStats = sessionManager.getStatistics()
        val channelStats = channelManager.getSystemStats()
        call.respondHtml {
            adminStatusPage(
                session = session,
                message = call.request.queryParameters["message"],
                sessionStats = sessionStats,
                channelStats = channelStats,
            )
        }
    }
}

private suspend fun ApplicationCall.adminSession(): AdminSession? {
    val session = sessions.get<AdminSession>()
    if (session?.authenticated == true) return session
    respondRedirect("/admin/login")
    return null
}

private suspend fun ApplicationCall.redirectWithMessage(path: String, message: String) {
    respondRedirect("$path?message=${encode(message)}")
}

private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

private fun validateSourceUrls(urls: List<String>): String? {
    if (urls.isEmpty()) return "至少填写一个 URL"

    for (raw in urls) {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return "非法 URL：$raw"
        if (uri.scheme != "http" && uri.scheme != "https") {
            return "只允许 http/https URL：$raw"
        }
        val host = uri.host ?: return "URL 缺少主机名：$raw"
        if (isPrivateHost(host)) {
            return "禁止使用内网/环回地址：$raw"
        }
    }
    return null
}

private fun isPrivateHost(host: String): Boolean {
    val value = host.removePrefix("[").removeSuffix("]").lowercase()
    if (value == "localhost" || value == "::1" || value == "::" || value == "0.0.0.0") return true
    if (value.startsWith("127.") || value.startsWith("10.") || value.startsWith("192.168.")) return true
    if (value.startsWith("169.254.")) return true
    if (value.contains(":") && (value.startsWith("fe80:") || value.startsWith("fc") || value.startsWith("fd"))) return true
    if (value.startsWith("::ffff:127.") || value.startsWith("::ffff:10.") ||
        value.startsWith("::ffff:192.168.")
    ) return true
    return Regex("^172\\.(1[6-9]|2\\d|3[0-1])\\.").containsMatchIn(value)
}

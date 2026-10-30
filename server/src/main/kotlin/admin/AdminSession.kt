package com.eimsound.eimusic.server.admin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable
import java.security.SecureRandom
import java.util.Base64

@Serializable
data class AdminSession(
    val authenticated: Boolean = false,
    val username: String = "",
    val csrf: String = "",
)

fun Application.configureAdminSessions() {
    val secret = env("EIMUSIC_ADMIN_SESSION_SECRET") ?: randomSecret()
    install(Sessions) {
        cookie<AdminSession>("EIMUSIC_ADMIN_SESSION", SessionStorageMemory()) {
            this.cookie.path = "/admin"
            this.cookie.httpOnly = true
            this.cookie.secure = env("EIMUSIC_ADMIN_SECURE_COOKIE")?.toBoolean() ?: false
            this.cookie.extensions["SameSite"] = "Strict"
            transform(SessionTransportTransformerMessageAuthentication(secret.toByteArray()))
        }
    }
}

private fun env(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: System.getProperty(name)?.takeIf { it.isNotBlank() }

private fun randomSecret(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

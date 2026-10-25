package com.eimsound.eimusic.server

import com.eimsound.eimusic.serialization.ProtoBuf
import com.eimsound.eimusic.server.handler.WebSocketHandler
import com.eimsound.eimusic.together.User
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false

        contentConverter = KotlinxWebsocketSerializationConverter(ProtoBuf)
    }

    routing {
        val handler by inject<WebSocketHandler>()
        // 认证后的一起听WebSocket路由
        authenticate("auth-jwt") {
            webSocket("/together") {
                // 此时连接已通过验证
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: throw IllegalArgumentException("Invalid userId")
                val username = principal?.payload?.getClaim("username")?.asString()
                    ?: throw IllegalArgumentException("Invalid username")
                val user = User(userId, username)
                handler.handleSession(this, user)
            }
        }
    }
}

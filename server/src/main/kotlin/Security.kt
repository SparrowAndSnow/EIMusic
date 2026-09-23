package com.eimsound.eimusic.server

import com.eimsound.eimusic.server.util.JwtUtil
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    // JWT配置参数
    val jwtRealm = "EIMusic Server"

    authentication {
        // 标准HTTP JWT认证
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JwtUtil.verifier
            )
            authHeader { call ->
                val token = call.request.queryParameters["token"]
                if (token != null) {
                    parseAuthorizationHeader("Bearer $token")
                } else {
                    call.request.parseAuthorizationHeader()
                }
            }
            validate { credential ->
                // 这里的 username 必须在 generateToken 时确实 put 进去了
                val username = credential.payload.getClaim("username").asString()
                if (!username.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}
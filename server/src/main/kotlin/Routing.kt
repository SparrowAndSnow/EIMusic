package com.eimsound.eimusic.server

import com.eimsound.eimusic.auth.AuthRequest
import com.eimsound.eimusic.auth.AuthResponse
import com.eimsound.eimusic.auth.AuthUser
import com.eimsound.eimusic.auth.RefreshRequest
import com.eimsound.eimusic.network.NeteaseApi
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.network.models.netease.NeteaseResult
import com.eimsound.eimusic.server.manager.ChannelManager
import com.eimsound.eimusic.server.manager.UserSessionManager
import com.eimsound.eimusic.server.util.JwtUtil
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    routing {
        val neteaseApi by inject<NeteaseApi>()
        // 健康检查
        get("/health") {
            call.respondText("OK")
        }

        route("/netease") {
            authenticate("auth-jwt") {
                post("/Song_V1") {
                    val body = call.receive<Map<String, String>>()
                    val url = body["url"] ?: return@post call.respond(HttpStatusCode.BadRequest, "url is required")
                    val level = body["level"] ?: "exhigh"
                    val type = body["type"] ?: "json"
                    when (val result = neteaseApi.getSongDetail(url, level, type)) {
                        is NetworkResult.Success -> call.respond(
                            NeteaseResult(
                                success = true,
                                data = result.data,
                                message = "success",
                                status = 200
                            )
                        )

                        else -> {
                            call.response.status(HttpStatusCode.InternalServerError)
                        }
                    }
                }

                post("/Search") {
                    val params = call.receiveParameters()
                    val keyword =
                        params["keyword"] ?: return@post call.respond(HttpStatusCode.BadRequest, "keyword is required")
                    val limit = params["limit"]?.toIntOrNull() ?: 10
                    when (val result = neteaseApi.search(keyword, limit)) {
                        is NetworkResult.Success -> call.respond(
                            NeteaseResult(
                                success = true,
                                data = result.data,
                                message = "success",
                                status = 200
                            )
                        )

                        else -> {
                            call.response.status(HttpStatusCode.InternalServerError)
                        }
                    }
                }
                get("/Playlist") {
                    val id = call.request.queryParameters["id"]
                        ?: call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "id is required")
                    when (val result = neteaseApi.getPlaylistDetail(id)) {
                        is NetworkResult.Success -> call.respond(
                            NeteaseResult(
                                success = true,
                                data = result.data,
                                message = "success",
                                status = 200
                            )
                        )

                        else -> {
                            call.response.status(HttpStatusCode.InternalServerError)
                        }
                    }
                }
                get("/Album") {
                    val id = call.request.queryParameters["id"]
                        ?: call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "id is required")
                    when (val result = neteaseApi.getAlbumDetail(id)) {
                        is NetworkResult.Success -> call.respond(
                            NeteaseResult(
                                success = true,
                                data = result.data,
                                message = "success",
                                status = 200
                            )
                        )

                        else -> {
                            call.response.status(HttpStatusCode.InternalServerError)
                        }
                    }
                }
            }
        }

        route("/auth") {
            post("/login") {
                val body = call.receive<AuthRequest>()

                // 模拟数据库验证（实际开发请连接 DB）
                if (body.username.isBlank()) return@post call.respond(HttpStatusCode.BadRequest)

                // 生成双令牌
                val accessToken = JwtUtil.generateToken(body.username, body.username, isRefresh = false)
                val refreshToken = JwtUtil.generateToken(body.username, body.username, isRefresh = true)

                call.respond(
                    AuthResponse(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresIn = JwtUtil.accessTokenExpiration,
                        user = AuthUser(id = body.username, username = body.username)
                    )
                )
            }
            post("/refresh") {
                val body = call.receive<RefreshRequest>()

                val decodedJWT = JwtUtil.verifyAndGetPrincipal(body.refreshToken, "Refresh")

                if (decodedJWT != null) {
                    val userId = decodedJWT.getClaim("userId").asString()
                    val username = decodedJWT.getClaim("username").asString()

                    val newAccessToken = JwtUtil.generateToken(userId, username, isRefresh = false)
                    val newRefreshToken = JwtUtil.generateToken(userId, username, isRefresh = true)

                    return@post call.respond(
                        AuthResponse(
                            accessToken = newAccessToken,
                            refreshToken = newRefreshToken,
                            expiresIn = JwtUtil.accessTokenExpiration,
                            user = AuthUser(id = userId, username = username)
                        )
                    )
                }

                call.respond(HttpStatusCode.Unauthorized, "Refresh Token 已失效")
            }
            authenticate("auth-jwt") {
                get("/me") {
                    val principal =
                        call.principal<JWTPrincipal>() ?: return@get call.respond(HttpStatusCode.Unauthorized)

                    val username = principal.payload.getClaim("username").asString()
                    val userId = principal.payload.getClaim("userId").asString()

                    call.respond(
                        AuthUser(
                            id = userId,
                            username = username,
                        )
                    )
                }
                val channelManager by inject<ChannelManager>()
                val userSessionManager by inject<UserSessionManager>()

                post("/logout") {
                    val principal =
                        call.principal<JWTPrincipal>() ?: return@post call.respond(HttpStatusCode.Unauthorized)

                    val username = principal.payload.getClaim("username").asString()
                    val userId = principal.payload.getClaim("userId").asString()
                    val session =
                        userSessionManager.getSessionByUserId(userId) ?: return@post call.respond(HttpStatusCode.OK)
                    session.currentChannelId?.let {
                        channelManager.leaveChannel(it, session)
                    }
                    userSessionManager.removeSession(session.sessionId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

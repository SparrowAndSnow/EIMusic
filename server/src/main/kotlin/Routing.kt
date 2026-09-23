package com.eimsound.eimusic.server

import com.eimsound.eimusic.PlatformType
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
import com.eimsound.eimusic.version.UpdateInfo
import com.eimsound.eimusic.version.UpdateResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

/**
 * 解析版本号字符串为可比较的列表
 */
fun parseVersion(version: String): List<Int> {
    return version
        .removePrefix("v")
        .removePrefix("V")
        .split(".")
        .mapNotNull { it.toIntOrNull() }
}

/**
 * 比较两个版本号
 */
fun compareVersions(version1: String, version2: String): Int {
    val v1Parts = parseVersion(version1)
    val v2Parts = parseVersion(version2)
    val maxLength = maxOf(v1Parts.size, v2Parts.size)
    for (i in 0 until maxLength) {
        val part1 = v1Parts.getOrElse(i) { 0 }
        val part2 = v2Parts.getOrElse(i) { 0 }
        if (part1 != part2) {
            return part1 - part2
        }
    }
    return 0
}

fun Application.configureRouting() {
    val logger = LoggerFactory.getLogger("Routing")

    routing {
        val neteaseApi by inject<NeteaseApi>()
        // 健康检查
        get("/health") {
            call.respondText("OK")
        }

        route("/version") {
            get("/check") {
                val currentVersion = call.request.queryParameters["version"]
                    ?: return@get call.response.status(HttpStatusCode.BadRequest)
                val platform = PlatformType.valueOf(
                    call.request.queryParameters["platform"]
                        ?: return@get call.response.status(HttpStatusCode.BadRequest)
                )
                val os = call.request.queryParameters["os"]
                    ?: return@get call.response.status(HttpStatusCode.BadRequest)
                // 从 BuildConfig 读取最新版本号
                val latestVersion = BuildConfig.VERSION_NAME
                val latestVersionCode = BuildConfig.VERSION_CODE
                val downloadUrl = when (platform) {
                    PlatformType.DESKTOP -> {
                        when {
                            os.startsWith("osx") -> BuildConfig.VERSION_DOWNLOAD_DESKTOP_MAC
                            os.startsWith("linux") -> BuildConfig.VERSION_DOWNLOAD_DESKTOP_LINUX
                            os.startsWith("windows") -> BuildConfig.VERSION_DOWNLOAD_DESKTOP_WINDOWS
                            else -> return@get call.response.status(HttpStatusCode.BadRequest)
                        }
                    }

                    PlatformType.ANDROID -> {
                        BuildConfig.VERSION_DOWNLOAD_ANDROID
                    }

                    PlatformType.IOS -> {
                        BuildConfig.VERSION_DOWNLOAD_IOS
                    }

                    PlatformType.WASM -> {
                        return@get call.response.status(HttpStatusCode.BadRequest)
                    }

                    else -> {
                        ""
                    }
                }

                // 比较版本号
                val hasUpdate = compareVersions(currentVersion, latestVersion) < 0

                val response = if (hasUpdate) {
                    UpdateResponse(
                        hasUpdate = true,
                        update = UpdateInfo(
                            version = latestVersion,
                            versionCode = latestVersionCode,
                            title = BuildConfig.VERSION_TITLE,
                            description = BuildConfig.VERSION_DESCRIPTION,
                            releaseDate = java.time.LocalDate.now().toString(),
                            downloadUrl = downloadUrl,
                            isCritical = BuildConfig.VERSION_FORCE,
                            releaseNotes = null,
                        ),
                        latestVersion = latestVersion
                    )
                } else {
                    UpdateResponse(
                        hasUpdate = false,
                        update = null,
                        latestVersion = latestVersion
                    )
                }

                call.respond(response)
            }
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
                    val id =
                        call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "id is required")
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
                    val id =
                        call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "id is required")
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

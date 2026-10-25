package com.eimsound.eimusic.network

import com.eimsound.eimusic.auth.AuthResponse
import com.eimsound.eimusic.auth.AuthStore
import com.eimsound.eimusic.auth.RefreshRequest
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.serialization.Json
import com.eimsound.eimusic.serialization.Json
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json

/**
 * 全局HTTP客户端配置
 */
class HttpClientProvider(
    private val refreshTokenUrl: String,
    private val authSore: AuthStore
) {
    val client: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json)
            }

            install(WebSockets){
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }

            install(HttpTimeout) {
                val timeout = 30000L
                connectTimeoutMillis = timeout
                requestTimeoutMillis = timeout
                socketTimeoutMillis = timeout
            }

            install(io.ktor.client.plugins.auth.Auth) {
                bearer {
                    cacheTokens = false
                    // 1. 设置初始 Access Token (从本地存储读取)
                    loadTokens {
                        authSore.getToken()
                    }

                    // 2. 当收到 401 时触发此逻辑
                    refreshTokens {
                        // 发送 HTTP 请求换取新 Token
                        // 注意：这里使用另一个独立的 client 或使用当前 client 但跳过 Auth 插件
                        val response = client.post(refreshTokenUrl) {
                            // 标记此请求不需要再次触发 Auth 流程，防止死循环
                            markAsRefreshTokenRequest()
                            setBody(RefreshRequest(oldTokens?.refreshToken ?: ""))
                            contentType(ContentType.Application.Json)
                        }

                        if (response.status == HttpStatusCode.OK) {
                            val newTokens = response.body<AuthResponse>()
                            val bearerTokens = BearerTokens(
                                accessToken = newTokens.accessToken,
                                refreshToken = newTokens.refreshToken
                            )
                            // 3. 保存新 Token 到本地存储
                            authSore.setToken(bearerTokens)

                            // 4. 返回给插件，它会自动重试之前失败的任务
                            bearerTokens
                        } else {
                            // 刷新失败（如 Refresh Token 也过期了），清除存储并引导用户重新登录
                            authSore.clear()
                            authSore.unauthenticated()
                            null
                        }
                    }
                }
            }
        }
    }

    suspend inline fun <reified T> request(
        crossinline block: suspend HttpClient.() -> HttpResponse
    ): NetworkResult<T> {
        return try {
            val response = block(client)
            if (response.status.isSuccess()) {
                NetworkResult.Success(response.body())
            } else {
                NetworkResult.Error(response.status.value, "Server error: ${response.status.description}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e.message)
        }
    }

}
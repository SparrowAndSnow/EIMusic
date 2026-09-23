package com.eimsound.eimusic.network

import com.eimsound.eimusic.auth.AuthRequest
import com.eimsound.eimusic.auth.AuthResponse
import com.eimsound.eimusic.auth.AuthUser
import com.eimsound.eimusic.network.models.NetworkResult
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BasicAuthProvider
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用户认证API实现
 */
class AuthApiImpl(
    private val baseUrl: String = "http://localhost:8080",
    private val client: HttpClientProvider,
) : AuthApi {


    override suspend fun login(username: String): NetworkResult<AuthResponse> = withContext(Dispatchers.Default) {
        client.request {
            post("${baseUrl}/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(username = username, password = "")) // 简化版本只需要用户名
            }
        }
    }

    override suspend fun refreshToken(refreshToken: String): NetworkResult<AuthResponse> =
        withContext(Dispatchers.Default) {
            client.request {
                post("${baseUrl}/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("refreshToken" to refreshToken))
                }
            }
        }

    override suspend fun getCurrentUser(): NetworkResult<AuthUser> = withContext(Dispatchers.Default) {
        client.request {
            get("${baseUrl}/auth/me")
        }
    }

    override suspend fun logout(): NetworkResult<Unit> = withContext(Dispatchers.Default) {
        client.client.authProvider<BasicAuthProvider>()?.clearToken()
        client.request {
            post("${baseUrl}/auth/logout")
        }
    }
}
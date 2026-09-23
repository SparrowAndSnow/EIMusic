package com.eimsound.eimusic.auth

import kotlinx.serialization.Serializable

/**
 * 认证请求模型
 */
@Serializable
data class AuthRequest(
    val username: String,
    val password: String
)

/**
 * 认证响应模型
 */
@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long, // 过期时间（秒）
    val user: AuthUser
)

/**
 * 认证用户信息
 */
@Serializable
data class AuthUser(
    val id: String,
    val username: String,
    val email: String? = "",
    val avatar: String? = ""
)

@Serializable
data class RefreshRequest(val refreshToken: String)
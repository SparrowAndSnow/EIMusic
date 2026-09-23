package com.eimsound.eimusic.server.util

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.*
/**
 * JWT工具类
 * 提供JWT令牌的生成、验证和解析功能
 */
object JwtUtil {
    // 建议：这些应该从 ApplicationConfig 获取，这里为了演示保持简洁
    private const val SECRET = "eimusic_jwt_secret_key_2026"
    private const val ISSUER = "EIMusicServer"
    private const val AUDIENCE = "eimusic_users"
    const val refreshTokenExpiration = 7 * 24 * 3600 * 1000L
    const val accessTokenExpiration = 1 * 60 * 1000L
    val algorithm = Algorithm.HMAC256(SECRET)

    // 提供给 Ktor Authentication 配置使用的公共 Verifier
    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()

    fun generateToken(userId: String, username: String, isRefresh: Boolean = false): String {
        val expiration = if (isRefresh) refreshTokenExpiration else accessTokenExpiration
        return JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(if (isRefresh) "Refresh" else "Authentication")
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + expiration))
            .sign(algorithm)
    }

    // 验证逻辑：直接返回 Principal 或 Null，减少重复逻辑
    fun verifyAndGetPrincipal(token: String, expectedSubject: String): DecodedJWT? {
        return try {
            val decoded = verifier.verify(token)
            if (decoded.subject == expectedSubject) decoded else null
        } catch (e: Exception) {
            null
        }
    }
}

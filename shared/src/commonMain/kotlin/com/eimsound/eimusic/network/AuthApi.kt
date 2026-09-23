package com.eimsound.eimusic.network

import com.eimsound.eimusic.auth.AuthRequest
import com.eimsound.eimusic.auth.AuthResponse
import com.eimsound.eimusic.auth.AuthUser
import com.eimsound.eimusic.network.models.NetworkResult

/**
 * 用户认证API接口
 */
interface AuthApi {
    /**
     * 用户登录
     * @param username 用户名
     * @return AuthResponse 包含JWT令牌和用户信息
     */
    suspend fun login(username: String): NetworkResult<AuthResponse>
    
    /**
     * 刷新访问令牌
     * @param refreshToken 刷新令牌
     * @return 新的AuthResponse
     */
    suspend fun refreshToken(refreshToken: String): NetworkResult<AuthResponse>
    
    /**
     * 获取当前用户信息
     * @return AuthUser 当前用户信息
     */
    suspend fun getCurrentUser(): NetworkResult<AuthUser>
    
    /**
     * 用户登出
     */
    suspend fun logout(): NetworkResult<Unit>
}
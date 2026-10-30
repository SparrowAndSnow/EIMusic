package com.eimsound.eimusic.server.admin

import com.eimsound.eimusic.server.db.UserRecord
import com.eimsound.eimusic.server.db.UserRepository

/**
 * 管理员账号服务。
 *
 * 管理员账号存放在 users 表；第一次访问管理端时通过初始化页面创建。
 */
class AdminAccountService(private val userRepository: UserRepository) {
    fun initializeFirstAdmin(username: String, password: String): UserRecord? =
        userRepository.createFirstAdmin(username, PasswordHasher.hash(password))

    fun authenticate(username: String, password: String): UserRecord? {
        val auth = userRepository.findAuthByUsername(username) ?: return null
        if (auth.role != ROLE_ADMIN || auth.disabled) return null
        if (!PasswordHasher.verify(password, auth.passwordHash)) return null
        return UserRecord(
            id = auth.id,
            username = auth.username,
            role = auth.role,
            disabled = auth.disabled,
            createdAt = auth.createdAt,
        )
    }

    fun hasAdmin(): Boolean = userRepository.hasRole(ROLE_ADMIN)

    companion object {
        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_USER = "USER"
    }
}

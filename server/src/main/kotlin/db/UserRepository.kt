package com.eimsound.eimusic.server.db

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

data class UserRecord(
    val id: Long,
    val username: String,
    val role: String,
    val disabled: Boolean,
    val createdAt: Long,
)

data class UserAuthRecord(
    val id: Long,
    val username: String,
    val passwordHash: String,
    val role: String,
    val disabled: Boolean,
    val createdAt: Long,
)

class UserRepository(private val database: Database) {
    fun all(): List<UserRecord> = transaction(database) {
        Users.selectAll()
            .map { it.toUserRecord() }
    }

    fun findById(id: Long): UserRecord? = transaction(database) {
        Users.selectAll()
            .where { Users.id eq id }
            .limit(1)
            .map { it.toUserRecord() }
            .singleOrNull()
    }

    fun findByUsername(username: String): UserRecord? = transaction(database) {
        Users.selectAll()
            .where { Users.username eq username }
            .limit(1)
            .map { it.toUserRecord() }
            .singleOrNull()
    }

    fun findAuthByUsername(username: String): UserAuthRecord? = transaction(database) {
        Users.selectAll()
            .where { Users.username eq username }
            .limit(1)
            .map { it.toAuthRecord() }
            .singleOrNull()
    }

    fun hasRole(role: String): Boolean = transaction(database) {
        Users.selectAll()
            .where { Users.role eq role }
            .limit(1)
            .toList()
            .isNotEmpty()
    }

    fun create(
        username: String,
        passwordHash: String = "",
        role: String = "USER",
    ): UserRecord = transaction(database) {
        val createdAt = System.currentTimeMillis()
        val id = Users.insert {
            it[Users.username] = username
            it[Users.passwordHash] = passwordHash
            it[Users.role] = role
            it[Users.disabled] = false
            it[Users.createdAt] = createdAt
        } get Users.id
        UserRecord(id, username, role, false, createdAt)
    }

    /**
     * 仅在没有管理员时创建第一个管理员，避免初始化接口被重复调用。
     */
    fun createFirstAdmin(username: String, passwordHash: String): UserRecord? = transaction(database) {
        val hasAdmin = Users.selectAll()
            .where { Users.role eq "ADMIN" }
            .limit(1)
            .toList()
            .isNotEmpty()
        if (hasAdmin) return@transaction null

        val createdAt = System.currentTimeMillis()
        val id = Users.insert {
            it[Users.username] = username
            it[Users.passwordHash] = passwordHash
            it[Users.role] = "ADMIN"
            it[Users.disabled] = false
            it[Users.createdAt] = createdAt
        } get Users.id
        UserRecord(id, username, "ADMIN", false, createdAt)
    }

    fun promoteToAdmin(id: Long, passwordHash: String) {
        transaction(database) {
            Users.update({ Users.id eq id }) {
                it[Users.role] = "ADMIN"
                it[Users.passwordHash] = passwordHash
            }
        }
    }

    fun updatePasswordHash(id: Long, passwordHash: String) {
        transaction(database) {
            Users.update({ Users.id eq id }) {
                it[Users.passwordHash] = passwordHash
            }
        }
    }

    fun setDisabled(id: Long, disabled: Boolean) {
        transaction(database) {
            Users.update({ Users.id eq id }) {
                it[Users.disabled] = disabled
            }
        }
    }

    private fun ResultRow.toUserRecord() = UserRecord(
        id = this[Users.id],
        username = this[Users.username],
        role = this[Users.role],
        disabled = this[Users.disabled],
        createdAt = this[Users.createdAt],
    )

    private fun ResultRow.toAuthRecord() = UserAuthRecord(
        id = this[Users.id],
        username = this[Users.username],
        passwordHash = this[Users.passwordHash],
        role = this[Users.role],
        disabled = this[Users.disabled],
        createdAt = this[Users.createdAt],
    )
}

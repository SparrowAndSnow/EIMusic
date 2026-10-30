package com.eimsound.eimusic.server.db

import org.jetbrains.exposed.v1.core.Table

/**
 * 服务端用户表。
 *
 * 目前只建立基础设施，登录逻辑仍保持原有 JWT 模拟实现；
 * 后续接入真实账号体系时，可直接通过 [UserRepository] 读写。
 */
object Users : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 64).uniqueIndex()
    val passwordHash = varchar("password_hash", 255).default("")
    val role = varchar("role", 16).default("USER")
    val disabled = bool("disabled").default(false)
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }

    override val primaryKey = PrimaryKey(id)
}

/**
 * 服务端音源配置。
 *
 * 同一 source 可以配置多个 URL，按 position 排序用于回退。
 */
object SourceUrls : Table("source_urls") {
    val sourceName = varchar("source", 32)
    val url = text("url")
    val position = integer("position").default(0)

    override val primaryKey = PrimaryKey(sourceName, url)
}

/**
 * 管理端操作审计。
 *
 * 只记录管理端动作，不记录密码等敏感信息。
 */
object AdminAudit : Table("admin_audit") {
    val id = long("id").autoIncrement()
    val actor = varchar("actor", 64)
    val action = varchar("action", 64)
    val detail = text("detail")
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }

    override val primaryKey = PrimaryKey(id)
}

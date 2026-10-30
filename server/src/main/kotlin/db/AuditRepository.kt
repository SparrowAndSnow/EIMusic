package com.eimsound.eimusic.server.db

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class AuditRecord(
    val id: Long,
    val actor: String,
    val action: String,
    val detail: String,
    val createdAt: Long,
)

class AuditRepository(private val database: Database) {
    fun record(actor: String, action: String, detail: String = ""): AuditRecord = transaction(database) {
        val createdAt = System.currentTimeMillis()
        val id = AdminAudit.insert {
            it[AdminAudit.actor] = actor
            it[AdminAudit.action] = action
            it[AdminAudit.detail] = detail
            it[AdminAudit.createdAt] = createdAt
        } get AdminAudit.id
        AuditRecord(id, actor, action, detail, createdAt)
    }

    fun recent(limit: Int = 100): List<AuditRecord> = transaction(database) {
        AdminAudit.selectAll()
            .map { it.toAuditRecord() }
            .sortedWith(compareByDescending<AuditRecord> { it.createdAt }.thenByDescending { it.id })
            .take(limit)
    }

    private fun ResultRow.toAuditRecord() = AuditRecord(
        id = this[AdminAudit.id],
        actor = this[AdminAudit.actor],
        action = this[AdminAudit.action],
        detail = this[AdminAudit.detail],
        createdAt = this[AdminAudit.createdAt],
    )
}

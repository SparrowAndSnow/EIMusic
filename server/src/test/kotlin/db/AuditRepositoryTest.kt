package com.eimsound.eimusic.server.db

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class AuditRepositoryTest {
    @Test
    fun recordsAndReturnsRecentFirst() {
        val file = Files.createTempFile("eimusic-audit-test", ".db")
        try {
            val database = ServerDatabase.connect(file)
            val repository = AuditRepository(database)

            repository.record("admin", "login", "success")
            repository.record("admin", "source.update", "NETEASE: https://a.example")

            val records = repository.recent()
            assertEquals(2, records.size)
            assertEquals("source.update", records[0].action)
            assertEquals("NETEASE: https://a.example", records[0].detail)
            assertEquals("login", records[1].action)
        } finally {
            Files.deleteIfExists(file)
        }
    }
}

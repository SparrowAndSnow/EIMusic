package com.eimsound.eimusic.server.db

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserRepositoryTest {
    @Test
    fun createAndFindUser() {
        val file = Files.createTempFile("eimusic-server-test", ".db")
        try {
            val database = ServerDatabase.connect(file)
            val repository = UserRepository(database)
            val created = repository.create("alice")

            assertEquals("alice", created.username)
            assertEquals(created, repository.findByUsername("alice"))
            assertNull(repository.findByUsername("bob"))
        } finally {
            Files.deleteIfExists(file)
        }
    }
}

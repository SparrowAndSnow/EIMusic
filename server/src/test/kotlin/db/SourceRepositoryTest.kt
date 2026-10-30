package com.eimsound.eimusic.server.db

import com.eimsound.eimusic.music.TrackSource
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceRepositoryTest {
    @Test
    fun seedsDefaultsAndCanReplace() {
        val file = Files.createTempFile("eimusic-source-test", ".db")
        try {
            val database = ServerDatabase.connect(file)
            val repository = SourceRepository(database)
            repository.ensureDefaults()

            assertEquals(
                listOf("https://netease-source.com"),
                repository.load()[TrackSource.NETEASE],
            )

            repository.replace(
                mapOf(TrackSource.NETEASE to listOf("https://a.example", "https://b.example")),
            )
            assertEquals(
                listOf("https://a.example", "https://b.example"),
                repository.load()[TrackSource.NETEASE],
            )
        } finally {
            Files.deleteIfExists(file)
        }
    }
}

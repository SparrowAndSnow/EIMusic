package com.eimsound.eimusic.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.eimsound.eimusic.music.TrackSource
import kotlin.test.Test
import kotlin.test.assertEquals

class DbLibraryRepositoryTest {
    private fun repository(): DbLibraryRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EIMusicDatabase.Schema.create(driver)
        return DbLibraryRepository(EIMusicDatabase(driver))
    }

    private fun track(id: String, title: String) = DbTrack(
        id = id,
        source = TrackSource.NETEASE,
        externalId = id.substringAfter(':'),
        title = title,
        artist = null,
        album = null,
        duration = null,
        uri = null,
        artUrl = null,
    )

    @Test
    fun recentIsNewestFirstAndDeduped() {
        val repo = repository()
        repo.recordRecent(track("netease:1", "one"))
        repo.recordRecent(track("netease:2", "two"))
        repo.recordRecent(track("netease:1", "one"))

        assertEquals(listOf("one", "two"), repo.recentTracks().map { it.title })
    }

    @Test
    fun searchHistoryIsNewestFirstAndDeduped() {
        val repo = repository()
        repo.recordSearch("first")
        repo.recordSearch("second")
        repo.recordSearch("first")

        assertEquals(listOf("first", "second"), repo.recentSearches())
    }
}

package com.eimsound.eimusic.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.eimsound.eimusic.music.TrackSource
import kotlin.test.Test
import kotlin.test.assertEquals

class DbSessionRepositoryTest {
    private fun repository(): DbSessionRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EIMusicDatabase.Schema.create(driver)
        return DbSessionRepository(EIMusicDatabase(driver))
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
    fun savesAndRestoresQueueAndCurrent() {
        val repo = repository()
        val queue = listOf(track("netease:1", "one"), track("netease:2", "two"))
        repo.saveTab(3)
        repo.savePlayback(queue, "netease:2")

        val session = repo.load()
        assertEquals(3, session.lastTab)
        assertEquals("netease:2", session.currentTrackId)
        assertEquals(listOf("one", "two"), session.queue.map { it.title })
    }

    @Test
    fun savingTabKeepsCurrentTrack() {
        val repo = repository()
        repo.savePlayback(listOf(track("netease:1", "one")), "netease:1")
        repo.saveTab(4)
        assertEquals("netease:1", repo.load().currentTrackId)
        assertEquals(4, repo.load().lastTab)
    }
}

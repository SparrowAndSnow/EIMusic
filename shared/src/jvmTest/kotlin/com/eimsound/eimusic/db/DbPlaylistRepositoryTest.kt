package com.eimsound.eimusic.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.eimsound.eimusic.music.TrackSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DbPlaylistRepositoryTest {
    private fun repository(): DbPlaylistRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EIMusicDatabase.Schema.create(driver)
        return DbPlaylistRepository(EIMusicDatabase(driver))
    }

    private fun track(id: String, source: TrackSource = TrackSource.NETEASE) = DbTrack(
        id = id,
        source = source,
        externalId = id.substringAfter(':'),
        title = "title-$id",
        artist = "artist",
        album = "album",
        duration = 1000,
        uri = null,
        artUrl = null,
    )

    @Test
    fun localPlaylistCrudKeepsOrder() {
        val repo = repository()
        val playlist = repo.createLocal("Mix")
        assertTrue(playlist.editable)

        repo.addTrack(playlist.id, track("netease:1"))
        repo.addTrack(playlist.id, track("netease:2"))
        repo.addTrack(playlist.id, track("netease:3"))
        assertEquals(listOf("netease:1", "netease:2", "netease:3"), repo.tracks(playlist.id).map { it.id })

        repo.moveTrack(playlist.id, 2, 0)
        assertEquals(listOf("netease:3", "netease:1", "netease:2"), repo.tracks(playlist.id).map { it.id })

        repo.removeTrack(playlist.id, "netease:1")
        assertEquals(listOf("netease:3", "netease:2"), repo.tracks(playlist.id).map { it.id })
    }

    @Test
    fun importedPlaylistIsReadOnlyAndSyncReplacesTracks() {
        val repo = repository()
        val imported = repo.importPlaylist(
            source = TrackSource.NETEASE,
            externalId = "42",
            name = "Imported",
            importedTracks = listOf(track("netease:1"), track("netease:2")),
        )
        assertTrue(!imported.editable)
        assertFailsWith<IllegalStateException> { repo.addTrack(imported.id, track("netease:3")) }

        repo.importPlaylist(TrackSource.NETEASE, "42", "Imported v2", listOf(track("netease:3")))
        assertEquals(listOf("netease:3"), repo.tracks(imported.id).map { it.id })
        assertEquals("Imported v2", repo.playlist(imported.id)?.name)
    }

    @Test
    fun localPlaylistCanMixSources() {
        val repo = repository()
        val playlist = repo.createLocal("Mixed")
        repo.addTrack(playlist.id, track("netease:1", TrackSource.NETEASE))
        repo.addTrack(playlist.id, track("spotify:abc", TrackSource.SPOTIFY))
        repo.addTrack(playlist.id, track("local:x", TrackSource.LOCAL))
        assertEquals(3, repo.tracks(playlist.id).size)
        assertEquals(TrackSource.SPOTIFY, repo.tracks(playlist.id)[1].source)
    }
}

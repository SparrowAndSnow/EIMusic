package com.eimsound.eimusic.db

import com.eimsound.eimusic.music.TrackSource
import kotlin.time.Clock
import kotlin.random.Random

/** 数据库里的曲目快照，id 为全局唯一字符串（如 netease:123 / local:...）。 */
data class DbTrack(
    val id: String,
    val source: TrackSource,
    val externalId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Long?,
    val uri: String?,
    val artUrl: String?,
)

/** 歌单；只有 source == LOCAL 才可编辑。 */
data class DbPlaylist(
    val id: String,
    val name: String,
    val source: TrackSource,
    val externalId: String?,
    val tracks: List<DbTrack>,
) {
    val editable: Boolean get() = source == TrackSource.LOCAL
}

class DbPlaylistRepository(
    private val db: EIMusicDatabase,
) {
    private val playlists get() = db.playlistQueries
    private val tracks get() = db.trackQueries

    fun localPlaylists(): List<DbPlaylist> =
        playlists.selectLocalPlaylists().executeAsList().map(::toPlaylist)

    fun importedPlaylists(): List<DbPlaylist> =
        playlists.selectImportedPlaylists().executeAsList().map(::toPlaylist)

    fun playlist(id: String): DbPlaylist? =
        playlists.selectPlaylist(id).executeAsOneOrNull()?.let(::toPlaylist)

    fun tracks(playlistId: String): List<DbTrack> =
        playlists.selectPlaylistTracks(playlistId).executeAsList().map(::toTrack)

    fun createLocal(name: String): DbPlaylist {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = "local-${now.toString(16)}-${Random.nextLong().toULong().toString(16)}"
        playlists.createPlaylist(id, name, TrackSource.LOCAL.name, null, now, now)
        return DbPlaylist(id, name, TrackSource.LOCAL, null, emptyList())
    }

    fun renameLocal(id: String, name: String) {
        requireEditable(id)
        playlists.renamePlaylist(name, Clock.System.now().toEpochMilliseconds(), id)
    }

    fun deleteLocal(id: String) {
        requireEditable(id)
        db.transaction {
            playlists.deletePlaylistTracks(id)
            playlists.deletePlaylist(id)
        }
    }

    fun addTrack(playlistId: String, track: DbTrack) {
        requireEditable(playlistId)
        if (tracks(playlistId).any { it.id == track.id }) return
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            upsertTrack(track)
            val position = playlists.maxPlaylistPosition(playlistId).executeAsOne() + 1
            playlists.insertPlaylistTrack(playlistId, track.id, position, now)
        }
    }

    fun removeTrack(playlistId: String, trackId: String) {
        requireEditable(playlistId)
        playlists.deletePlaylistTrack(playlistId, trackId)
        compactPositions(playlistId)
    }

    fun moveTrack(playlistId: String, from: Int, to: Int) {
        requireEditable(playlistId)
        val current = tracks(playlistId).toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        db.transaction {
            current.forEachIndexed { index, track ->
                playlists.updatePlaylistTrackPosition(index.toLong(), playlistId, track.id)
            }
        }
    }

    /** 导入/同步提供方歌单：按 (source, external_id) upsert，整体替换曲目。 */
    fun importPlaylist(
        source: TrackSource,
        externalId: String,
        name: String,
        importedTracks: List<DbTrack>,
    ): DbPlaylist {
        require(source != TrackSource.LOCAL) { "local playlists are created locally" }
        val now = Clock.System.now().toEpochMilliseconds()
        val existing = playlists.selectPlaylistByExternal(source.name, externalId).executeAsOneOrNull()
        val id = existing?.id ?: "${source.name.lowercase()}-$externalId"
        db.transaction {
            if (existing == null) {
                playlists.createPlaylist(id, name, source.name, externalId, now, now)
            } else {
                playlists.renamePlaylist(name, now, id)
            }
            playlists.deletePlaylistTracks(id)
            importedTracks.forEachIndexed { index, track ->
                upsertTrack(track)
                playlists.insertPlaylistTrack(id, track.id, index.toLong(), now)
            }
        }
        return DbPlaylist(id, name, source, externalId, importedTracks)
    }

    private fun requireEditable(id: String) {
        val playlist = playlists.selectPlaylist(id).executeAsOneOrNull()
            ?: error("playlist not found: $id")
        check(playlist.source == TrackSource.LOCAL.name) { "imported playlist is read-only" }
    }

    private fun compactPositions(playlistId: String) {
        val current = tracks(playlistId)
        db.transaction {
            current.forEachIndexed { index, track ->
                playlists.updatePlaylistTrackPosition(index.toLong(), playlistId, track.id)
            }
        }
    }

    private fun upsertTrack(track: DbTrack) {
        tracks.upsertTrack(
            id = track.id,
            source = track.source.name,
            external_id = track.externalId,
            title = track.title,
            artist = track.artist,
            album = track.album,
            duration = track.duration,
            uri = track.uri,
            art_url = track.artUrl,
        )
    }

    private fun toPlaylist(row: Playlist): DbPlaylist = DbPlaylist(
        id = row.id,
        name = row.name,
        source = trackSource(row.source),
        externalId = row.external_id,
        tracks = tracks(row.id),
    )

    private fun toTrack(row: Track): DbTrack = DbTrack(
        id = row.id,
        source = trackSource(row.source),
        externalId = row.external_id,
        title = row.title,
        artist = row.artist,
        album = row.album,
        duration = row.duration,
        uri = row.uri,
        artUrl = row.art_url,
    )

    private fun trackSource(value: String): TrackSource =
        runCatching { TrackSource.valueOf(value) }.getOrDefault(TrackSource.UNKNOWN)
}

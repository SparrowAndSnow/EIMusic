package com.eimsound.eimusic.data

import com.eimsound.eimusic.db.DbPlaylist
import com.eimsound.eimusic.db.DbSession
import com.eimsound.eimusic.db.DbTrack
import com.eimsound.eimusic.db.LibraryRepository
import com.eimsound.eimusic.db.PlaylistRepository
import com.eimsound.eimusic.db.SessionRepository
import com.eimsound.eimusic.music.TrackSource
import kotlin.time.Clock

/** Browser-only, page-lifetime persistence. Reloading the page clears these values. */
internal object InMemoryRepositories {
    val playlist: PlaylistRepository = MemoryPlaylists()
    val library: LibraryRepository = MemoryLibrary()
    val session: SessionRepository = MemorySession()
}

private class MemoryPlaylists : PlaylistRepository {
    private val playlists = linkedMapOf<String, DbPlaylist>()
    private fun ordered(source: TrackSource) = playlists.values.filter { it.source == source }
    override fun localPlaylists() = ordered(TrackSource.LOCAL)
    override fun importedPlaylists() = playlists.values.filter { it.source != TrackSource.LOCAL }
    override fun playlist(id: String) = playlists[id]
    override fun tracks(playlistId: String) = playlists[playlistId]?.tracks.orEmpty()
    override fun createLocal(name: String): DbPlaylist {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = "local-${now.toString(16)}-${playlists.size}"
        return DbPlaylist(id, name, TrackSource.LOCAL, null, emptyList()).also { playlists[id] = it }
    }
    override fun renameLocal(id: String, name: String) {
        val old = playlists[id] ?: error("playlist not found: $id")
        check(old.editable) { "imported playlist is read-only" }
        playlists[id] = old.copy(name = name)
    }
    override fun deleteLocal(id: String) {
        val old = playlists[id] ?: error("playlist not found: $id")
        check(old.editable) { "imported playlist is read-only" }
        playlists.remove(id)
    }
    override fun deletePlaylistRecord(id: String) { playlists.remove(id) }
    override fun addTrack(playlistId: String, track: DbTrack) {
        val old = editable(playlistId)
        if (old.tracks.none { it.id == track.id }) playlists[playlistId] = old.copy(tracks = old.tracks + track)
    }
    override fun removeTrack(playlistId: String, trackId: String) {
        val old = editable(playlistId)
        playlists[playlistId] = old.copy(tracks = old.tracks.filterNot { it.id == trackId })
    }
    override fun moveTrack(playlistId: String, from: Int, to: Int) {
        val old = editable(playlistId)
        if (from !in old.tracks.indices || to !in old.tracks.indices) return
        val tracks = old.tracks.toMutableList().apply { add(to, removeAt(from)) }
        playlists[playlistId] = old.copy(tracks = tracks)
    }
    override fun importPlaylist(source: TrackSource, externalId: String, name: String, importedTracks: List<DbTrack>): DbPlaylist {
        require(source != TrackSource.LOCAL) { "local playlists are created locally" }
        val id = playlists.values.firstOrNull { it.source == source && it.externalId == externalId }?.id ?: "${source.name.lowercase()}-$externalId"
        return DbPlaylist(id, name, source, externalId, importedTracks).also { playlists[id] = it }
    }
    private fun editable(id: String): DbPlaylist {
        val value = playlists[id] ?: error("playlist not found: $id")
        check(value.editable) { "imported playlist is read-only" }
        return value
    }
}

private class MemoryLibrary : LibraryRepository {
    private val recentTracks = mutableListOf<Pair<Long, DbTrack>>()
    private val searches = mutableListOf<Pair<Long, String>>()
    override fun recentTracks(limit: Int) = recentTracks.sortedByDescending { it.first }.take(limit).map { it.second }
    override fun recordRecent(track: DbTrack) {
        recentTracks.removeAll { it.second.id == track.id }
        recentTracks += Clock.System.now().toEpochMilliseconds() to track
    }
    override fun recentSearches(limit: Int) = searches.sortedByDescending { it.first }.take(limit).map { it.second }
    override fun recordSearch(query: String) {
        searches.removeAll { it.second == query }
        searches += Clock.System.now().toEpochMilliseconds() to query
    }
}

private class MemorySession : SessionRepository {
    private var lastTab = 0
    private var currentTrackId: String? = null
    private var queue = emptyList<DbTrack>()
    override fun load() = DbSession(lastTab, currentTrackId, queue.toList())
    override fun saveTab(lastTab: Int) { this.lastTab = lastTab }
    override fun savePlayback(queue: List<DbTrack>, currentTrackId: String?) {
        this.queue = queue.toList()
        this.currentTrackId = currentTrackId
    }
}

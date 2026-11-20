package com.eimsound.eimusic.db

import com.eimsound.eimusic.music.TrackSource

/** Persistence contracts used by shared stores; platform implementations may be transient. */
interface PlaylistRepository {
    fun localPlaylists(): List<DbPlaylist>
    fun importedPlaylists(): List<DbPlaylist>
    fun playlist(id: String): DbPlaylist?
    fun tracks(playlistId: String): List<DbTrack>
    fun createLocal(name: String): DbPlaylist
    fun renameLocal(id: String, name: String)
    fun deleteLocal(id: String)
    fun deletePlaylistRecord(id: String)
    fun addTrack(playlistId: String, track: DbTrack)
    fun removeTrack(playlistId: String, trackId: String)
    fun moveTrack(playlistId: String, from: Int, to: Int)
    fun importPlaylist(source: TrackSource, externalId: String, name: String, importedTracks: List<DbTrack>): DbPlaylist
}

interface LibraryRepository {
    fun recentTracks(limit: Int = 12): List<DbTrack>
    fun recordRecent(track: DbTrack)
    fun recentSearches(limit: Int = 8): List<String>
    fun recordSearch(query: String)
}

interface SessionRepository {
    fun load(): DbSession
    fun saveTab(lastTab: Int)
    fun savePlayback(queue: List<DbTrack>, currentTrackId: String?)
}

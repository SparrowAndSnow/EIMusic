package com.eimsound.eimusic.db

import com.eimsound.eimusic.music.TrackSource
import kotlin.time.Clock

class DbLibraryRepository(
    private val db: EIMusicDatabase,
) {
    private val recent get() = db.recentQueries
    private val tracks get() = db.trackQueries

    fun recentTracks(limit: Int = 12): List<DbTrack> =
        recent.selectRecent(limit.toLong()).executeAsList().map(::toTrack)

    fun recordRecent(track: DbTrack) {
        db.transaction {
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
            recent.upsertRecent(track.id, Clock.System.now().toEpochMilliseconds())
        }
    }

    fun recentSearches(limit: Int = 8): List<String> =
        recent.selectSearches(limit.toLong()).executeAsList()

    fun recordSearch(query: String) {
        recent.upsertSearch(query, Clock.System.now().toEpochMilliseconds())
    }

    private fun toTrack(row: Track): DbTrack = DbTrack(
        id = row.id,
        source = runCatching { TrackSource.valueOf(row.source) }.getOrDefault(TrackSource.UNKNOWN),
        externalId = row.external_id,
        title = row.title,
        artist = row.artist,
        album = row.album,
        duration = row.duration,
        uri = row.uri,
        artUrl = row.art_url,
    )
}

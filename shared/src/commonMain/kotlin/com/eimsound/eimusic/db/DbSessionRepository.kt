package com.eimsound.eimusic.db

import com.eimsound.eimusic.music.TrackSource

data class DbSession(
    val lastTab: Int,
    val currentTrackId: String?,
    val queue: List<DbTrack>,
)

class DbSessionRepository(
    private val db: EIMusicDatabase,
) {
    private val session get() = db.sessionQueries
    private val tracks get() = db.trackQueries

    fun load(): DbSession {
        val row = session.selectSession().executeAsOneOrNull()
        return DbSession(
            lastTab = row?.last_tab?.toInt() ?: 0,
            currentTrackId = row?.current_track_id,
            queue = session.selectQueue().executeAsList().map(::toTrack),
        )
    }

    fun saveTab(lastTab: Int) {
        val currentTrackId = session.selectSession().executeAsOneOrNull()?.current_track_id
        session.upsertSession(lastTab.toLong(), currentTrackId)
    }

    fun savePlayback(queue: List<DbTrack>, currentTrackId: String?) {
        val existingTab = session.selectSession().executeAsOneOrNull()?.last_tab?.toInt() ?: 0
        db.transaction {
            queue.forEach { track ->
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
            session.upsertSession(existingTab.toLong(), currentTrackId)
            session.deleteQueueItems()
            queue.forEachIndexed { index, track ->
                session.insertQueueItem(index.toLong(), track.id)
            }
        }
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

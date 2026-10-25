package com.eimsound.eimusic.util

import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId
import com.eimsound.eimusic.music.TrackSource

val audioExtensions = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "wma")

fun localTack(path: String, name: String, uri: String): Track {
    return Track(
        id = TrackId(TrackSource.LOCAL, path),
        name = name,
        uri = uri,
        artists = emptyList(),
        album = null,
        duration = null,
        source = TrackSource.LOCAL
    )
}

expect suspend fun loadTrackFiles(dirs: Set<String>): List<Track>

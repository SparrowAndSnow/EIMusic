package com.eimsound.eimusic.util

import com.eimsound.eimusic.music.Artist
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId
import com.eimsound.eimusic.music.TrackSource

val audioExtensions = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "wma")

fun localTrack(path: String): Track {
    val name = path.split("/").lastOrNull()?.substringBeforeLast(".") ?: path
    return Track(
        id = TrackId(TrackSource.LOCAL, path),
        name = name,
        uri = path,
        source = TrackSource.LOCAL,
        album = null,
        artists = emptyList(),
        duration = null,
    )
}

expect suspend fun loadTrackFiles(dirs: Set<String>): List<Track>

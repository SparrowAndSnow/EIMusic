package com.eimsound.eimusic.db

import com.eimsound.eimusic.music.Album
import com.eimsound.eimusic.music.Artist
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId

fun DbTrack.toTrack(): Track = Track(
    album = if (album != null || artUrl != null) {
        Album(
            artists = emptyList(),
            id = "",
            image = artUrl,
            name = album.orEmpty(),
            releaseDate = null,
            totalTracks = 0,
            tracks = emptyList(),
        )
    } else {
        null
    },
    artists = artist?.let { listOf(Artist(image = null, id = source.name, name = it)) } ?: emptyList(),
    duration = duration?.let(::Duration),
    id = runCatching { TrackId.parse(id) }.getOrElse { TrackId(source, externalId) },
    name = title,
    uri = uri,
    source = source,
)

fun Track.toDbTrack(): DbTrack = DbTrack(
    id = id.toString(),
    source = id.source,
    externalId = id.id,
    title = name,
    artist = artists?.joinToString("/") { it.name },
    album = album?.name,
    duration = duration?.milliseconds,
    uri = uri,
    artUrl = album?.image,
)

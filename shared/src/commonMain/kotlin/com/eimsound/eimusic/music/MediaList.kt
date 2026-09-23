package com.eimsound.eimusic.music

import kotlinx.serialization.Serializable

sealed interface MediaList {
    val source: TrackSource
    val type: MediaListType
    val id: MediaListId

    data class PlaylistMediaList(
        val playlist: Playlist,
        override val source: TrackSource,
        override val type: MediaListType = MediaListType.PLAYLIST,
        override val id: MediaListId,
    ) : MediaList {
        fun toUniversalMediaList() = UniversalMediaList(
            id = id,
            tracks = playlist.tracks,
            name = playlist.name,
            creator = playlist.creator,
            image = playlist.image,
            totalTracks = playlist.totalTracks,
            description = playlist.description
        )
    }

    data class AlbumMediaList(
        val album: Album,
        override val source: TrackSource,
        override val type: MediaListType = MediaListType.ALBUM,
        override val id: MediaListId,
    ) : MediaList{
        fun toUniversalMediaList() = UniversalMediaList(
            id = id,
            tracks = album.tracks,
            name = album.name,
            creator = album.artists.joinToString { it.name },
            image = album.image,
            totalTracks = album.totalTracks,
            description = album.releaseDate
        )
    }
}

@Serializable
data class MediaListId(val source: TrackSource, val type: MediaListType, val id: String) {
    companion object {
        fun parse(id: String): MediaListId {
            val split = id.split(":")
            return MediaListId(TrackSource.valueOf(split[0]), MediaListType.valueOf(split[1]), split[2])
        }
    }

    override fun toString(): String = "$source:$type:$id"
}

enum class MediaListType {
    PLAYLIST, ALBUM
}

data class UniversalMediaList(
    val id: MediaListId,
    val tracks: List<Track> = emptyList(),
    val name: String,
    val creator: String? = null,
    val image: String? = null,
    val totalTracks: Int = 0,
    val description: String? = null
)

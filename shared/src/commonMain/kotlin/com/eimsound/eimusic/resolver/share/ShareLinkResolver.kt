package com.eimsound.eimusic.resolver.share

import com.eimsound.eimusic.music.MediaListId
import com.eimsound.eimusic.music.MediaListType
import com.eimsound.eimusic.music.TrackSource

interface ShareLinkResolver {
    suspend fun resolve(link: String): ShareLinkType
}

sealed interface ShareLinkType {
    val source: TrackSource
    val link: String

    data class PlaylistLink(val id: String, override val source: TrackSource, override val link: String) : ShareLinkType

    data class AlbumLink(val id: String, override val source: TrackSource, override val link: String) : ShareLinkType

    data class TrackLink(val id: String, override val source: TrackSource, override val link: String) : ShareLinkType

    data class UnknownLink(override val source: TrackSource, override val link: String) : ShareLinkType
}

fun ShareLinkType.toMediaListType(): MediaListType? {
    return when (this) {
        is ShareLinkType.AlbumLink -> MediaListType.ALBUM
        is ShareLinkType.PlaylistLink -> MediaListType.PLAYLIST
        else -> null
    }
}
fun ShareLinkType.toMediaListId(): MediaListId? {
    return when (this) {
        is ShareLinkType.AlbumLink -> MediaListId(source, MediaListType.ALBUM, id)
        is ShareLinkType.PlaylistLink -> MediaListId(source, MediaListType.PLAYLIST, id)
        else -> null
    }
}
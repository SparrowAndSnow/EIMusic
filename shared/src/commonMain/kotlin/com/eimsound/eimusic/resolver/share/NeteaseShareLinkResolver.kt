package com.eimsound.eimusic.resolver.share

import com.eimsound.eimusic.music.TrackSource

class NeteaseShareLinkResolver : ShareLinkResolver {
    val patterns = listOf<Pair<Regex, (String, String) -> ShareLinkType>>(
        NeteaseShareLinkRegex.playlistRegex to { id, link ->
            ShareLinkType.PlaylistLink(id, TrackSource.NETEASE, link)
        },
        NeteaseShareLinkRegex.albumRegex to { id, link ->
            ShareLinkType.AlbumLink(id, TrackSource.NETEASE, link)
        },
        NeteaseShareLinkRegex.trackRegex to { id, link ->
            ShareLinkType.TrackLink(id, TrackSource.NETEASE, link)
        }
    )

    override suspend fun resolve(link: String): ShareLinkType {
        for ((regex, createType) in patterns) {
            val matchResult = regex.find(link)
            val id = matchResult?.groupValues?.get(1)
            if (id != null) return createType(id, link)
        }

        return ShareLinkType.UnknownLink(TrackSource.NETEASE, link)
    }
}

object NeteaseShareLinkRegex {
    val playlistRegex = "playlist\\?id=(\\d+)".toRegex()
    val albumRegex = "album\\?id=(\\d+)".toRegex()
    val trackRegex = "song\\?id=(\\d+)".toRegex()
}
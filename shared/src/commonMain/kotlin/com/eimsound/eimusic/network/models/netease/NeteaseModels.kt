package com.eimsound.eimusic.network.models.netease

import com.eimsound.eimusic.music.Album
import com.eimsound.eimusic.music.Artist
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.Playlist
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId
import com.eimsound.eimusic.music.TrackSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
data class NeteaseResult<T>(
    val data: T,
    val message: String,
    val status: Int,
    val success: Boolean
)

@Serializable
data class NeteaseSearch(
    @SerialName("duration") val durationMs: Long? = null,
    @SerialName("dt") val dt: Long? = null,
    val album: String? = null,
    @SerialName("artist_string")
    val artistString: String? = null,
    val artists: String? = null,
    val id: Long? = null,
    val name: String? = null,
    val picUrl: String? = null,
)


@Serializable
data class NeteaseSong(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("al_name")
    val alName: String? = null,
    @SerialName("ar_name")
    val arName: String? = null,
    @SerialName("duration") val durationMs: Long? = null,
    @SerialName("dt") val dt: Long? = null,
    @SerialName("level")
    val level: String? = null,
    @SerialName("lyric")
    val lyric: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("pic")
    val pic: String? = null,
    @SerialName("size")
    val size: String? = null,
    @SerialName("tlyric")
    val tlyric: String? = null,
    @SerialName("url")
    val url: String? = null
)

@Serializable
data class NeteaseArtist(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("picUrl")
    val picUrl: String? = null
)

@Serializable
data class NeteaseAlbumResponse(val album: NeteaseAlbum?, val status: String?)

@Serializable
data class NeteaseAlbumSong(
    @SerialName("duration") val durationMs: Long? = null,
    @SerialName("dt") val dt: Long? = null,
    @SerialName("album")
    val album: String? = null,
    @SerialName("artists")
    val artists: String? = null,
    @SerialName("id")
    val id: Long? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("picUrl")
    val picUrl: String? = null,
)

@Serializable
data class NeteaseAlbum(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("artist")
    val artist: String? = null,
    @SerialName("coverImgUrl")
    val coverImgUrl: String? = null,
    @SerialName("publishTime")
    val publishTime: Long? = null,
    @SerialName("description")
    val description: String? = null,

    val songs: List<NeteaseAlbumSong>? = null
)


@Serializable
data class NeteasePlaylistResponse(val playlist: NeteasePlaylist?, val status: String?)

@Serializable
data class NeteasePlaylist(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("coverImgUrl")
    val coverImgUrl: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("trackCount")
    val trackCount: Int? = null,
    @SerialName("creator")
    val creator: String? = null,
    @SerialName("tracks")
    val tracks: List<NeteasePlaylistSong>? = null
)

@Serializable
data class NeteasePlaylistSong(
    @SerialName("duration") val durationMs: Long? = null,
    @SerialName("dt") val dt: Long? = null,
    val album: String? = null,
    val artists: String? = null,
    val id: Long? = null,
    val name: String? = null,
    val picUrl: String? = null,
)


/**
 * 扩展函数：将网易云数据模型转换为应用内部模型
 */
fun NeteaseSong.toTrack(): Track {
    val artistsList = listOf(
        Artist(
            id = arName ?: "",
            name = arName ?: "",
            image = null
        )
    )

    val albumObj = Album(
        id = alName ?: "",
        name = alName ?: "",
        image = pic,
        releaseDate = null,
        totalTracks = 0,
        artists = artistsList,
        tracks = emptyList()
    )
    return Track(
        id = TrackId(TrackSource.NETEASE, id?.toString() ?: toString()),
        name = name ?: "",
        artists = artistsList,
        album = albumObj,
        duration = (durationMs ?: dt)?.let(::Duration),
        uri = url,
        source = TrackSource.NETEASE
    )
}

@JvmName("listNeteaseSongToTrack")

fun List<NeteaseSong>.toTracks(): List<Track> = this.map { it.toTrack() }

@JvmName("listNeteaseSearchToTrack")
fun List<NeteaseSearch>.toTracks(): List<Track> {
    return this.map {
        val artist = Artist(
            id = it.artistString ?: "",
            name = it.artists ?: "",
            image = null
        )
        Track(
            id = TrackId(TrackSource.NETEASE, it.id?.toString() ?: toString()),
            name = it.name ?: "",
            artists = listOf(artist),
            album = Album(
                id = it.album ?: "",
                name = it.album ?: "",
                image = it.picUrl,
                releaseDate = null,
                totalTracks = 0,
                artists = listOf(artist),
                tracks = emptyList()
            ),
            duration = (it.durationMs ?: it.dt)?.let(::Duration),
            uri = null,
            source = TrackSource.NETEASE
        )
    }
}

fun NeteasePlaylistSong.toTrack(): Track {
    val artist = Artist(
        id = artists ?: "",
        name = artists ?: "",
        image = null
    )
    return Track(
        id = TrackId(TrackSource.NETEASE, id?.toString() ?: toString()),
        name = name ?: "",
        artists = listOf(artist),
        album = Album(
            id = album ?: "",
            name = album ?: "",
            image = picUrl,
            releaseDate = null,
            totalTracks = 0,
            artists = listOf(artist),
            tracks = emptyList()
        ),
        duration = (durationMs ?: dt)?.let(::Duration),
        uri = null,
        source = TrackSource.NETEASE
    )
}

fun List<NeteasePlaylistSong>.toTracks(): List<Track> = this.map { it.toTrack() }

fun NeteaseAlbum.toAlbum(): Album {
    val artistsList = listOf(
        Artist(
            id = artist ?: "",
            name = artist ?: "",
            image = coverImgUrl
        )
    )
    return Album(
        id = id?.toString() ?: "",
        name = name ?: "",
        image = coverImgUrl,
        releaseDate = publishTime?.toString(),
        totalTracks = songs?.size ?: 0,
        artists = artistsList,
        tracks = songs?.toTracks() ?: emptyList()
    )
}

@JvmName("neteaseAlbumSongToTrack")
fun List<NeteaseAlbumSong>.toTracks(): List<Track> {
    return this.map {
        val artist = Artist(
            id = it.artists ?: "",
            name = it.artists ?: "",
            image = null
        )
        Track(
            id = TrackId(TrackSource.NETEASE, it.id?.toString() ?: toString()),
            name = it.name ?: "",
            artists = listOf(artist),
            album = Album(
                id = it.album ?: "",
                name = it.album ?: "",
                image = it.picUrl,
                releaseDate = null,
                totalTracks = 0,
                artists = listOf(artist),
                tracks = emptyList()
            ),
            duration = (it.durationMs ?: it.dt)?.let(::Duration),
            uri = null,
            source = TrackSource.NETEASE
        )
    }
}

fun NeteasePlaylist.toPlaylist(): Playlist {
    return Playlist(
        id = id?.toString() ?: "",
        name = name ?: "",
        creator = creator,
        image = coverImgUrl,
        description = description,
        tracks = tracks?.toTracks() ?: emptyList(),
        totalTracks = trackCount ?: 0
    )
}

fun NeteaseAlbum.toPlaylist(): Playlist {
    return Playlist(
        id = id?.toString() ?: "",
        name = name ?: "",
        creator = artist,
        image = coverImgUrl,
        description = description,
        tracks = songs?.toTracks() ?: emptyList(),
        totalTracks = songs?.size ?: 0
    )
}
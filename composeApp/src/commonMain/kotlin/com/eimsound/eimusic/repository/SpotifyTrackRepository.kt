package com.eimsound.eimusic.repository

import com.eimsound.eimusic.music.Album
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.Playlist
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.network.SpotifyApiImpl
import com.eimsound.eimusic.Result
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.MediaListType

/**
 * 基于Spotify API的具体音轨仓库实现
 */
class SpotifyTrackRepository(
    private val spotifyApi: SpotifyApiImpl = SpotifyApiImpl()
) : TrackRepository {

    private suspend fun loadTopTracks(): List<Track> {
        return spotifyApi.getTopFiftyChart().tracks?.items?.map { item ->
            val trackDto = item.track
            val artists = trackDto?.album?.artists?.map { artistDto ->
                com.eimsound.eimusic.music.Artist(
                    name = artistDto.name ?: "",
                    id = artistDto.id ?: "",
                    image = artistDto.uri
                )
            }

            Track(
                album = trackDto?.album?.let { albumDto ->
                    Album(
                        name = albumDto.name ?: "",
                        image = albumDto.images?.firstOrNull()?.url.orEmpty(),
                        releaseDate = albumDto.releaseDate,
                        id = albumDto.id ?: "",
                        totalTracks = albumDto.totalTracks ?: 0,
                        artists = artists ?: emptyList(),
                        tracks = emptyList()
                    )
                },
                artists = artists ?: emptyList(),
                name = trackDto?.name ?: "",
                uri = trackDto?.previewUrl,
                duration = Duration(trackDto?.durationMs?.toLong()?.div(1000) ?: 0),
                id = TrackId(TrackSource.SPOTIFY, trackDto?.id ?: trackDto.toString()),
                source = TrackSource.SPOTIFY
            )
        } ?: emptyList()
    }

    override suspend fun universalMediaList(
        mediaListType: MediaListType,
        id: String
    ): Result<MediaList> {
        TODO("Not yet implemented")
    }

    override suspend fun search(
        query: String,
        limit: Int
    ): Result<List<Track>> {
        TODO("Not yet implemented")
    }

    override suspend fun track(id: String): Result<Track?> {
        TODO("Not yet implemented")
    }

    override suspend fun playlist(id: String): Result<Playlist?> {
        TODO("Not yet implemented")
    }

    override suspend fun album(id: String): Result<Album?> {
        TODO("Not yet implemented")
    }

}
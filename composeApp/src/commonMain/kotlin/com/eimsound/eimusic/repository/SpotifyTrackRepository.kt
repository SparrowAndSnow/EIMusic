package com.eimsound.eimusic.repository

import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.network.SpotifyApiImpl

/**
 * 基于Spotify API的具体音轨仓库实现
 */
class SpotifyTrackRepository(
    private val spotifyApi: SpotifyApiImpl = SpotifyApiImpl()
) : TrackRepository {
    
    override suspend fun loadTracks(params: Map<String, Any>): List<Track> {
        // 根据参数加载不同类型的音轨列表
        val type = params["type"] as? String ?: "top"
        
        return when (type) {
            "top" -> loadTopTracks()
            else -> emptyList()
        }
    }
    
    override suspend fun searchTracks(query: String): List<Track> {
        // 使用Spotify API搜索音轨
        return emptyList()
    }
    

    private suspend fun loadTopTracks(): List<Track> {
        return spotifyApi.getTopFiftyChart().tracks?.items?.map { item ->
            val trackDto = item.track
            val artists = trackDto?.album?.artists?.map { artistDto ->
                com.eimsound.eimusic.music.Artist(
                    name = artistDto.name,
                    id = artistDto.id,
                    image = artistDto.uri
                )
            }
            
            Track(
                album = trackDto?.album?.let { albumDto ->
                    com.eimsound.eimusic.music.Album(
                        name = albumDto.name,
                        image = albumDto.images?.firstOrNull()?.url.orEmpty(),
                        releaseDate = albumDto.releaseDate,
                        id = albumDto.id,
                        totalTracks = albumDto.totalTracks,
                        artists = artists
                    )
                },
                artists = artists,
                name = trackDto?.name,
                uri = trackDto?.previewUrl,
                duration = com.eimsound.eimusic.Duration(trackDto?.durationMs?.toLong()?.div(1000) ?: 0),
                id = trackDto?.id,
                isLocal = trackDto?.isLocal ?: false,
            )
        } ?: emptyList()
    }

}
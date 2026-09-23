package com.eimsound.eimusic.repository

import com.eimsound.eimusic.music.Album
import com.eimsound.eimusic.music.Playlist
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.network.NeteaseApi
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.network.models.netease.toAlbum
import com.eimsound.eimusic.network.models.netease.toPlaylist
import com.eimsound.eimusic.network.models.netease.toTrack
import com.eimsound.eimusic.network.models.netease.toTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.eimsound.eimusic.Result
import com.eimsound.eimusic.Result.*
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.MediaList.*
import com.eimsound.eimusic.music.MediaListId
import com.eimsound.eimusic.music.MediaListType
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.resolver.share.NeteaseShareLinkRegex

/**
 * 网易云音乐仓库实现
 * 提供网易云音乐相关数据的访问接口
 */
class NeteaseTrackRepository(
    private val neteaseApi: NeteaseApi,
) : TrackRepository {
    private suspend fun mediaListAlbum(id: String): Result<MediaList> {
        return when (val album = album(id)) {
            is Success -> {
                val album = album.data ?: return Error("专辑不存在")
                Success(
                    AlbumMediaList(
                        album, TrackSource.NETEASE,
                        MediaListType.ALBUM,
                        MediaListId(TrackSource.NETEASE, MediaListType.ALBUM, album.id)
                    )
                )
            }

            is Error -> album
        }
    }

    private suspend fun mediaListPlaylist(
        id: String
    ): Result<MediaList> {
        return when (val playlist = playlist(id)) {
            is Success -> {
                val playlist = playlist.data ?: return Error("歌单不存在")
                Success(
                    PlaylistMediaList(
                        playlist,
                        TrackSource.NETEASE,
                        MediaListType.PLAYLIST,
                        MediaListId(TrackSource.NETEASE, MediaListType.PLAYLIST, playlist.id)
                    )
                )
            }

            is Error -> playlist
        }
    }

    override suspend fun universalMediaList(
        mediaListType: MediaListType,
        id: String
    ): Result<MediaList> = withContext(Dispatchers.Default){
        when (mediaListType) {
            MediaListType.PLAYLIST -> {
                mediaListPlaylist(id)
            }

            MediaListType.ALBUM -> {
                mediaListAlbum(id)
            }
        }
    }

    override suspend fun search(
        query: String,
        limit: Int
    ): Result<List<Track>> = withContext(Dispatchers.Default) {
        when (val search = neteaseApi.search(query, limit)) {
            is NetworkResult.Success -> {
                Result.Success(search.data.toTracks())
            }

            is NetworkResult.Error -> {
                Result.Error(search.message)
            }

            is NetworkResult.Exception -> {
                Result.Error(search.message ?: "未知错误")
            }
        }
    }

    override suspend fun track(id: String): Result<Track?> = withContext(Dispatchers.Default) {
        val find = NeteaseShareLinkRegex.trackRegex.find(id)
        val id = find?.groupValues[1] ?: id
        when (val song = neteaseApi.getSongDetail(id)) {
            is NetworkResult.Success -> {
                Result.Success(song.data.toTrack())
            }

            is NetworkResult.Error -> {
                Result.Error(song.message)

            }

            is NetworkResult.Exception -> {
                Result.Error(song.message ?: "未知错误")
            }
        }
    }

    override suspend fun playlist(id: String): Result<Playlist?> = withContext(Dispatchers.Default) {
        val find = NeteaseShareLinkRegex.playlistRegex.find(id)
        val id = find?.groupValues[1] ?: id
        when (val playlist = neteaseApi.getPlaylistDetail(id)) {
            is NetworkResult.Success -> {
                Result.Success(data = playlist.data.playlist?.toPlaylist())
            }

            is NetworkResult.Error -> {
                Result.Error(playlist.message)
            }

            is NetworkResult.Exception -> {
                Result.Error(playlist.message ?: "未知错误")
            }
        }
    }

    override suspend fun album(id: String): Result<Album?> = withContext(Dispatchers.Default) {
        val find = NeteaseShareLinkRegex.albumRegex.find(id)
        val id = find?.groupValues[1] ?: id
        when (val album = neteaseApi.getAlbumDetail(id)) {
            is NetworkResult.Success -> {
                Result.Success(album.data.album?.toAlbum())
            }

            is NetworkResult.Error -> {
                Result.Error(album.message)
            }

            is NetworkResult.Exception -> {
                Result.Error(album.message ?: "未知错误")
            }
        }
    }

}

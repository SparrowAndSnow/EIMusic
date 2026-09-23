package com.eimsound.eimusic.repository

import com.eimsound.eimusic.music.Album
import com.eimsound.eimusic.music.Playlist
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.Result
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.MediaListType

/**
 * 音轨仓库接口，定义了对音轨的各种操作
 */
interface TrackRepository {
    suspend fun universalMediaList(mediaListType: MediaListType, id: String): Result<MediaList>

    suspend fun search(query: String, limit: Int = 30): Result<List<Track>>

    suspend fun track(id: String): Result<Track?>

    suspend fun playlist(id: String): Result<Playlist?>

    suspend fun album(id: String): Result<Album?>

}
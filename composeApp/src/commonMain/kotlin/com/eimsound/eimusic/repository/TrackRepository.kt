package com.eimsound.eimusic.repository

import com.eimsound.eimusic.music.Track

/**
 * 音轨仓库接口，定义了对音轨的各种操作
 */
interface TrackRepository {
    /**
     * 加载音轨列表
     * @param params 加载参数
     * @return 音轨列表
     */
    suspend fun loadTracks(params: Map<String, Any>): List<Track>

    /**
     * 搜索音轨
     * @param query 搜索关键字
     * @return 符合条件的音轨列表
     */
    suspend fun searchTracks(query: String): List<Track>

}
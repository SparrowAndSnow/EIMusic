package com.eimsound.eimusic.network

import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.network.models.netease.*

/**
 * 网易云音乐API接口定义
 */
interface NeteaseApi {

    /**
     * 搜索音乐
     * @param keywords 搜索关键词
     * @param limit 返回数量限制，默认30
     * @return 搜索结果
     */
    suspend fun search(
        keywords: String,
        limit: Int = 30,
    ): NetworkResult<List<NeteaseSearch>>

    /**
     * 获取歌曲详情
     * @param ids 歌曲ID列表
     * @return 歌曲详情列表
     */
    suspend fun getSongDetail(
        url: String,
        level: String = "exhigh",
        type: String = "json"
    ): NetworkResult<NeteaseSong>


    /**
     * 获取歌单详情
     * @param id 歌单ID
     * @return 歌单详情
     */
    suspend fun getPlaylistDetail(id: String): NetworkResult<NeteasePlaylistResponse>

    /**
     * 获取专辑详情
     * @param id 专辑ID
     * @return 专辑详情
     */
    suspend fun getAlbumDetail(id: String): NetworkResult<NeteaseAlbumResponse>

}
package com.eimsound.eimusic.network

import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.models.DownloadState
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.UpdateResponse
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow

/**
 * 更新检测 API 接口
 */
interface UpdateApi {
    /**
     * 检查更新
     * @param currentVersion 当前版本号
     * @param platformType 平台标识 (desktop, android, ios)
     * @return 更新响应
     */
    suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String
    ): NetworkResult<UpdateResponse>

    suspend fun download(url: String, destinationPath: String): Flow<DownloadState>
}
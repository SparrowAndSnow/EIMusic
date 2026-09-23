package com.eimsound.eimusic.util

import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.UpdateApi
import com.eimsound.eimusic.network.models.DownloadState
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.UpdateInfo
import com.eimsound.eimusic.version.UpdateResponse
import kotlinx.coroutines.flow.Flow

expect class UpdateService(updateApi: UpdateApi) {
    suspend fun executeUpdate(updateInfo: UpdateInfo): Flow<DownloadState>
    suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String
    ): NetworkResult<UpdateResponse>
}


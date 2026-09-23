package com.eimsound.eimusic.util

import com.eimsound.eimusic.AppVersion
import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.UpdateApi
import com.eimsound.eimusic.network.models.DownloadState
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.UpdateInfo
import com.eimsound.eimusic.version.UpdateResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class UpdateService actual constructor(private val updateApi: UpdateApi){
    actual suspend fun executeUpdate(updateInfo: UpdateInfo): Flow<DownloadState> {
        val downloadUrl = updateInfo.downloadUrl ?: return flow {
            emit(DownloadState.Error(Error("No download url")))
        }
        return updateApi.download(downloadUrl, "${AppVersion.combineName}.zip")
    }

    actual suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String
    ): NetworkResult<UpdateResponse> {
        return updateApi.checkUpdate(currentVersion, platformType, os)
    }
}
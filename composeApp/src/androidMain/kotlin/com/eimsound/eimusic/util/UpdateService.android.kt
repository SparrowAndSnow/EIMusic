package com.eimsound.eimusic.util

import android.content.Intent
import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.UpdateApi
import com.eimsound.eimusic.network.models.DownloadState
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.UpdateInfo
import com.eimsound.eimusic.version.UpdateResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import androidx.core.net.toUri
import com.eimsound.eimusic.EIMusicApplication

actual class UpdateService actual constructor(private val updateApi: UpdateApi) {
    actual suspend fun executeUpdate(updateInfo: UpdateInfo): Flow<DownloadState> {
        val url = updateInfo.downloadUrl ?: return flow {
            emit(DownloadState.Error(Error("No download url")))
        }
        if (url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return flow {
                emit(DownloadState.Error(Error("Invalid download url")))
            }
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            EIMusicApplication.instance.startActivity(intent)
        } catch (e: Exception) {
            return flow {
                emit(DownloadState.Error(Error("Failed to open download url")))
            }
        }
        return flow {
            emit(DownloadState.None)
        }
    }

    actual suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String
    ): NetworkResult<UpdateResponse> {
        return updateApi.checkUpdate(currentVersion, platformType, os);
    }
}
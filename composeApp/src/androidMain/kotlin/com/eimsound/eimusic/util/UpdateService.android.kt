package com.eimsound.eimusic.util

import android.content.Intent
import androidx.core.net.toUri
import com.eimsound.eimusic.EIMusicApplication
import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.UpdateApi
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.UpdateInfo
import com.eimsound.eimusic.version.UpdateResponse

actual class UpdateService actual constructor(private val updateApi: UpdateApi) {
    actual suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String
    ): NetworkResult<UpdateResponse> = updateApi.checkUpdate(currentVersion, platformType, os)

    actual suspend fun openDownload(updateInfo: UpdateInfo): Boolean {
        val url = updateInfo.downloadUrl ?: return false
        if (!url.startsWith("https://") && !url.startsWith("http://")) return false
        return try {
            EIMusicApplication.instance.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}

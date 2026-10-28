package com.eimsound.eimusic.util

import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.UpdateApi
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.UpdateInfo
import com.eimsound.eimusic.version.UpdateResponse
import kotlinx.browser.window

actual class UpdateService actual constructor(private val updateApi: UpdateApi) {
    actual suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String
    ): NetworkResult<UpdateResponse> = updateApi.checkUpdate(currentVersion, platformType, os)

    actual suspend fun openDownload(updateInfo: UpdateInfo): Boolean {
        val url = updateInfo.downloadUrl ?: return false
        return try {
            window.open(url, "_blank")
            true
        } catch (_: Throwable) {
            false
        }
    }
}

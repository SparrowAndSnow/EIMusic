package com.eimsound.eimusic.util

import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.UpdateApi
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.UpdateInfo
import com.eimsound.eimusic.version.UpdateResponse
import java.awt.Desktop
import java.net.URI

actual class UpdateService actual constructor(private val updateApi: UpdateApi) {
    actual suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String
    ): NetworkResult<UpdateResponse> = updateApi.checkUpdate(currentVersion, platformType, os)

    actual suspend fun openDownload(updateInfo: UpdateInfo): Boolean {
        val url = updateInfo.downloadUrl ?: return false
        if (!url.startsWith("https://") && !url.startsWith("http://")) return false
        return runCatching {
            require(Desktop.isDesktopSupported()) { "Desktop is not supported" }
            Desktop.getDesktop().browse(URI(url))
        }.isSuccess
    }
}

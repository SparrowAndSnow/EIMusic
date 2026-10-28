package com.eimsound.eimusic.util

import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.UpdateApi
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.UpdateInfo
import com.eimsound.eimusic.version.UpdateResponse

expect class UpdateService(updateApi: UpdateApi) {
    suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String
    ): NetworkResult<UpdateResponse>

    /** 打开版本下载页；各平台只负责跳转，不自行实现安装流程。 */
    suspend fun openDownload(updateInfo: UpdateInfo): Boolean
}

package com.eimsound.eimusic.network

import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.UpdateResponse

/** 更新检测 API。 */
interface UpdateApi {
    suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String
    ): NetworkResult<UpdateResponse>
}

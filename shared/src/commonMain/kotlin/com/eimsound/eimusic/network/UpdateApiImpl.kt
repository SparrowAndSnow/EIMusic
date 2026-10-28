package com.eimsound.eimusic.network

import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.UpdateResponse
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class UpdateApiImpl(
    private val baseUrl: String,
    private val httpClient: HttpClientProvider,
) : UpdateApi {
    override suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String
    ): NetworkResult<UpdateResponse> = httpClient.request {
        get("$baseUrl/version/check") {
            parameter("version", currentVersion)
            parameter("platform", platformType)
            parameter("os", os)
        }
    }
}

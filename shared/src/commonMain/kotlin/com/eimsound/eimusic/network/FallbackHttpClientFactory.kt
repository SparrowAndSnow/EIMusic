package com.eimsound.eimusic.network

import com.eimsound.eimusic.serialization.Json
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException

object FallbackHttpClientFactory {
    fun create(
        requestUrlProvider: RequestUrlProvider,
        onUrlSwitch: ((failed: Url, next: Url) -> Unit)? = null,
    ): HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }
        install(HttpRequestRetry) {
            maxRetries = 3
            retryOnExceptionIf { _, cause -> cause is IOException }
            retryIf { _, response ->
                response.status.value >= 400
            }
            modifyRequest {
                val failedUrl = request.url.build()
                val oldBase = requestUrlProvider.url.value
                val next = requestUrlProvider.next(failedUrl)

                it.url.protocol = next.protocol
                it.url.host = next.host
                it.url.port = next.port

                // Preserve the endpoint path segments when switching base URLs
                // e.g. /netease/Search → /Search when switching from /netease to /
                val endpointSegments = failedUrl.segments.drop(oldBase.segments.size)
                it.url.encodedPath = (next.segments + endpointSegments)
                    .joinToString("/", prefix = "/")

                onUrlSwitch?.invoke(failedUrl, next)
            }
        }
    }
}

package com.eimsound.eimusic.network

import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.models.DownloadState
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.UpdateResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * 更新检测 API 实现
 */
class UpdateApiImpl(
    private val baseUrl: String,
    private val httpClient: HttpClientProvider,
) : UpdateApi {

    override suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String
    ): NetworkResult<UpdateResponse> {
        return httpClient.request {
            get("$baseUrl/version/check") {
                parameter("version", currentVersion)
                parameter("platform", platformType)
                parameter("os", os)
            }
        }
    }

    override suspend fun download(url: String, destinationPath: String): Flow<DownloadState> = callbackFlow {
        try {
            trySend(DownloadState.Loading)
            httpClient.client.prepareGet(url).execute { response ->
                val contentLength = response.contentLength() ?: -1L
                val channel: ByteReadChannel = response.bodyAsChannel()
                val path = Path(destinationPath)
                path.parent?.let { SystemFileSystem.createDirectories(it) }

                SystemFileSystem.sink(path).buffered().use { sink ->
                    var totalBytesRead = 0L
                    val buffer = ByteArray(8192) // 8KB 缓冲区

                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer)
                        if (read == -1) break

                        sink.write(buffer, 0, read)
                        totalBytesRead += read

                        if (contentLength > 0) {
                            val progress = ((totalBytesRead.toDouble() / contentLength) * 100).toInt()
                            trySend(DownloadState.Progress(progress.coerceIn(0, 100)))
                        }
                    }
                    sink.flush()
                }
                if (response.status.isSuccess()) {
                    trySend(DownloadState.Success(destinationPath))
                } else {
                    throw Exception("HTTP Error: ${response.status}")
                }
            }
            close()
        } catch (e: Exception) {
            trySend(DownloadState.Error(e))
            close(e)
        }
    }.flowOn(Dispatchers.Default)
}

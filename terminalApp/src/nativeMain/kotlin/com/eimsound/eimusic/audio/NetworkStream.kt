package com.eimsound.eimusic.audio

import com.eimsound.eimusic.util.TuiLogger
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * 管理 HTTP 音频流的下载、缓存写入和内存缓冲。
 *
 * 启动后同时做两件事：
 * 1. 将下载的 chunk 写入 [cachePath] 缓存文件
 * 2. 将下载的 chunk 追加到 [StreamBuffer] 供 decoder 读取
 *
 * 调用 [waitForPrebuffer] 等待预缓冲阈值到达，然后可开始播放。
 * 播放完成后 [close] 释放资源。
 */
class NetworkStream(
    private val url: String,
    private val httpClient: HttpClient,
    private val cachePath: String? = null,
) {
    private val innerBuffer = StreamBuffer()
    private val prebufferThreshold = MIN_PREBUFFER

    /** 获取底层的 [StreamBuffer]，供 engine.openStream() 使用 */
    fun getBuffer(): StreamBuffer = innerBuffer

    /** 启动 HTTP 下载（挂起函数，应在后台协程中运行） */
    suspend fun start() {
        TuiLogger.log("NetworkStream: starting download $url")
        var cacheSink: kotlinx.io.Sink? = null
        try {
            httpClient.prepareGet(url).execute { response ->
                TuiLogger.log("NetworkStream: connected, status=${response.status}")
                cacheSink = cachePath?.let {
                    SystemFileSystem.sink(Path(it)).buffered()
                }
                val channel = response.bodyAsChannel()

                @OptIn(InternalAPI::class)
                while (!channel.isClosedForRead) {
                    currentCoroutineContext().ensureActive()
                    val chunk = ByteArray(CHUNK_SIZE)
                    val size = channel.readAvailable(chunk)
                    if (size <= 0) continue
                    val bytes = if (size == chunk.size) chunk else chunk.copyOf(size)

                    cacheSink?.write(bytes)
                    innerBuffer.append(bytes)
                }
                TuiLogger.log("NetworkStream: download complete")
            }
        } catch (e: Exception) {
            TuiLogger.log("NetworkStream: download failed: ${e.message}")
            innerBuffer.markError(e.message ?: "Unknown error")
        } finally {
            cacheSink?.close()
            innerBuffer.close()
        }
    }

    /**
     * 等待预缓冲阈值。
     * 轮询 [StreamBuffer.available] 直到达到 [minBytes] 或流关闭。
     */
    suspend fun waitForPrebuffer(minBytes: Int = prebufferThreshold) {
        TuiLogger.log("NetworkStream: waiting for prebuffer ($minBytes bytes)...")
        while (innerBuffer.available() < minBytes && !isClosed()) {
            delay(100)
        }
        val available = innerBuffer.available()
        TuiLogger.log("NetworkStream: prebuffer done ($available bytes available)")
    }

    /** 是否已关闭（下载完成或出错） */
    fun isClosed(): Boolean = innerBuffer.isClosed()

    /** 释放所有资源 */
    fun close() {
        innerBuffer.dispose()
    }

    companion object {
        const val MIN_PREBUFFER = 65536 // 64KB
        private const val CHUNK_SIZE = 65536 // 下载块大小
    }
}

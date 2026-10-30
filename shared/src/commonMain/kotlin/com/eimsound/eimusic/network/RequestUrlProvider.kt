package com.eimsound.eimusic.network

import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class RequestUrlProvider(urls: List<String>) {
    private val _urls = MutableStateFlow(urls.map { Url(it) })
    private var index = AtomicInt(0)
    private val _url = MutableStateFlow(
        requireNotNull(_urls.value.firstOrNull()) { "At least one source URL is required" }
    )

    val url
        get() = _url.asStateFlow()

    /**
     * 管理端修改音源后调用，立即切换运行中的客户端，不需要重启服务。
     */
    fun updateUrls(urls: List<String>) {
        val parsed = urls.map { Url(it) }
        require(parsed.isNotEmpty()) { "At least one source URL is required" }
        _urls.value = parsed
        index.store(0)
        _url.value = parsed.first()
    }

    private fun Url.toBaseAddress(): String {
        val protocolPart = protocol.name
        val portPart = if (port != protocol.defaultPort && port != 0) ":$port" else ""
        return "$protocolPart://$host$portPart"
    }

    fun next(failedUrl: Url): Url {
        val failedUrlStr = failedUrl.toBaseAddress()
        val urls = _urls.value
        val startIndex = index.load()

        while (true) {
            val currentIndex = index.load().coerceIn(0, urls.lastIndex)
            val activeUrlStr = urls[currentIndex].toBaseAddress()

            if (activeUrlStr != failedUrlStr) {
                return _url.value
            }

            val newIndex = (currentIndex + 1) % urls.size

            // 防止死循环：如果已经遍历了一圈，停止切换
            if (newIndex == startIndex) {
                return _url.value
            }

            if (index.compareAndSet(currentIndex, newIndex)) {
                _url.value = urls[newIndex]
                return _url.value
            }
            // CAS 失败说明其他线程改了 index，重试
        }
    }

}

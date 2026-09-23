package com.eimsound.eimusic.network

import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class RequestUrlProvider(urls: List<String>) {
    private val _urls = urls.map { Url(it) }
    private var index = AtomicInt(0)
    private val _url = MutableStateFlow(_urls[0])

    val url
        get() = _url.asStateFlow()

    private fun Url.toBaseAddress(): String {
        val protocolPart = protocol.name
        val portPart = if (port != protocol.defaultPort && port != 0) ":$port" else ""
        return "$protocolPart://$host$portPart"
    }

    fun next(failedUrl: Url): Url {
        val failedUrlStr = failedUrl.toBaseAddress()
        val startIndex = index.load()

        while (true) {
            val currentIndex = index.load()
            val activeUrlStr = _urls[currentIndex].toBaseAddress()

            if (activeUrlStr != failedUrlStr) {
                return _url.value
            }

            val newIndex = (currentIndex + 1) % _urls.size

            // 防止死循环：如果已经遍历了一圈，停止切换
            if (newIndex == startIndex) {
                return _url.value
            }

            if (index.compareAndSet(currentIndex, newIndex)) {
                _url.value = _urls[newIndex]
                return _url.value
            }
            // CAS 失败说明其他线程改了 index，重试
        }
    }

}

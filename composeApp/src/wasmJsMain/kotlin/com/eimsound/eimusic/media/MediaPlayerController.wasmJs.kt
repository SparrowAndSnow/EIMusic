package com.eimsound.eimusic.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import com.eimsound.eimusic.Duration

/**
 * Web平台的媒体播放控制器实现
 * 目前仅作为占位符，待后续实现完整功能
 */
actual class MediaPlayerController {
    actual fun prepare(source: String, listener: MediaPlayerListener) {
    }

    actual fun start() {
    }

    actual fun pause() {
    }

    actual fun stop() {
    }

    actual val isPlaying: Boolean
        get() = TODO("Not yet implemented")
    actual var volume: Double
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val position: Duration?
        get() = TODO("Not yet implemented")
    actual val duration: Duration?
        get() = TODO("Not yet implemented")

    actual fun release() {
    }

    actual fun seek(seconds: Duration, seekOver: () -> Unit) {
    }

    actual var isMuted: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

}

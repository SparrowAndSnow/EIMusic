package com.eimsound.eimusic.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import com.eimsound.eimusic.Duration

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
    actual val currentPosition: Duration?
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

@Composable
actual fun rememberMediaPlayerController(): MediaPlayerController {
    return rememberSaveable { MediaPlayerController() }
}
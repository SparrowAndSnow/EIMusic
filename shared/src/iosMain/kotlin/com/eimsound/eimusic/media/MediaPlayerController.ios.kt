package com.eimsound.eimusic.media

actual class MediaPlayerController {
    actual fun prepare(source: String, listener: MediaPlayerListener) {
    }

    actual fun start() {
    }

    actual fun pause() {
    }

    actual fun stop() {
    }

    actual val currentPosition: Long?
        get() = TODO("Not yet implemented")
    actual val duration: Long?
        get() = TODO("Not yet implemented")
    actual val isPlaying: Boolean
        get() = TODO("Not yet implemented")
    actual var volume: Double
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun seek(seconds: Long) {
    }

    actual fun release() {
    }

}

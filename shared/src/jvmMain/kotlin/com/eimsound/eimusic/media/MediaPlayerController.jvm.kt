package com.eimsound.eimusic.media

import javafx.scene.media.MediaPlayer
import javafx.util.Duration

actual class MediaPlayerController {
    private var mediaPlayer: MediaPlayer? = null

    actual fun prepare(source: String, listener: MediaPlayerListener) {
        mediaPlayer = createMediaPlayer(source, listener)
    }

    actual fun start() {
        mediaPlayer?.play()
    }

    actual fun pause() {
        mediaPlayer?.pause()
    }

    actual fun stop() {
        mediaPlayer?.stop()
    }

    actual val currentPosition: Long?
        get() = mediaPlayer?.currentTime?.toSeconds()?.toLong() ?: 0
    actual val duration: Long?
        get() = mediaPlayer?.totalDuration?.toSeconds()?.toLong() ?: 0
    actual val isPlaying: Boolean
        get() = mediaPlayer?.status == MediaPlayer.Status.PLAYING
    actual var volume: Double
        get() = mediaPlayer?.volume ?: 1.0
        set(value) {
            mediaPlayer?.volume = value
        }

    actual fun seek(seconds: Long) {
        mediaPlayer?.seek(Duration(seconds.toDouble() * 1000))
    }

    actual fun release() {
        mediaPlayer?.dispose()
    }
}

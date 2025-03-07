package com.eimsound.eimusic.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import com.eimsound.eimusic.Duration
import javafx.scene.media.MediaPlayer
import java.util.concurrent.CompletableFuture

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

    actual val position: Duration?
        get() = Duration(mediaPlayer?.currentTime?.toSeconds()?.toLong() ?: 0)
    actual val duration: Duration?
        get() = Duration(mediaPlayer?.totalDuration?.toSeconds()?.toLong() ?: 0)
    actual val isPlaying: Boolean
        get() = mediaPlayer?.status == MediaPlayer.Status.PLAYING
    actual var volume: Double
        get() = mediaPlayer?.volume ?: 1.0
        set(value) {
            mediaPlayer?.volume = value
        }
    actual var isMuted: Boolean
        get() = mediaPlayer?.isMute ?: false
        set(value) {
            mediaPlayer?.isMute = value
        }

    actual fun seek(seconds: Duration, seekOver: () -> Unit) {
        CompletableFuture.runAsync {
            mediaPlayer?.seek(javafx.util.Duration.seconds(seconds.seconds.toDouble()))
        }.thenRun {
            seekOver()
        }
    }

    actual fun release() {
        mediaPlayer?.dispose()
        mediaPlayer = null
    }
}


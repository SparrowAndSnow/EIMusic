package com.eimsound.eimusic.media

import androidx.compose.runtime.Composable
import com.eimsound.eimusic.Duration


expect class MediaPlayerController {
    fun prepare(source: String, listener: MediaPlayerListener)

    fun start()

    fun pause()

    fun stop()

    val currentPosition: Duration?

    val duration: Duration?

    val isPlaying: Boolean

    var volume: Double

    var isMuted: Boolean

    fun seek(seconds: Duration)

    fun release()
}

@Composable
expect fun rememberMediaPlayerController(): MediaPlayerController
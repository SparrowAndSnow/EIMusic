package com.eimsound.eimusic.media

import com.eimsound.eimusic.Duration


expect class MediaPlayerController() {
    fun prepare(source: String, listener: MediaPlayerListener)

    fun start()

    fun pause()

    fun stop()

    val position: Duration?

    val duration: Duration?

    val isPlaying: Boolean

    var volume: Double

    var isMuted: Boolean

    fun seek(seconds: Duration, seekOver: () -> Unit = {})

    fun release()
}

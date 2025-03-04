package com.eimsound.eimusic.media


expect class MediaPlayerController {
    fun prepare(source: String, listener: MediaPlayerListener)

    fun start()

    fun pause()

    fun stop()

    val currentPosition: Long?

    val duration: Long?

    val isPlaying: Boolean

    var volume: Double

    fun seek(seconds: Long)

    fun release()
}

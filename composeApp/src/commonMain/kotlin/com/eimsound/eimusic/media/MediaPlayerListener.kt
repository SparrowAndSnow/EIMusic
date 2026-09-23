package com.eimsound.eimusic.media

import com.eimsound.eimusic.music.Duration

interface MediaPlayerListener {
    fun onReady()
    fun onAudioCompleted()
    fun onError(message: String?)
    fun timer(duration: Duration)
    fun playStateChanged(isPlaying: Boolean)
    fun onLoading()
    fun onLoaded()
    fun onBufferProgress(progress: Float)
}
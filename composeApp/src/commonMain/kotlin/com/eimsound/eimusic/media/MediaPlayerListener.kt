package com.eimsound.eimusic.media

import com.eimsound.eimusic.Duration

interface MediaPlayerListener {
    fun onReady()
    fun onAudioCompleted()
    fun onError()
    fun timer(duration: Duration)
}

package com.eimsound.eimusic.media

interface MediaPlayerListener {
    fun onReady()
    fun onAudioCompleted()
    fun onError()
}

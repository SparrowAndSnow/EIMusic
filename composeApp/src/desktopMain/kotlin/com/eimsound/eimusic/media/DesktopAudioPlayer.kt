package com.eimsound.eimusic.media

import com.eimsound.eimusic.Duration
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

fun createMediaPlayer(source: String, listener: MediaPlayerListener): MediaPlayer {
    val media = Media(source)
    val mediaPlayer = MediaPlayer(media).apply {
        setOnReady {
            listener.onReady()
        }
        setOnEndOfMedia {
            listener.onAudioCompleted()
        }
        currentTimeProperty().addListener { _, _, newValue -> listener.timer(Duration(newValue.toSeconds().toLong())) }
        setOnError {
            listener.onError()
        }
        statusProperty().addListener {
            _, _, newValue ->
            when(newValue){
                MediaPlayer.Status.STALLED -> listener.onLoading()
                else -> { listener.onLoaded() }
            }
        }
    }
    return mediaPlayer
}


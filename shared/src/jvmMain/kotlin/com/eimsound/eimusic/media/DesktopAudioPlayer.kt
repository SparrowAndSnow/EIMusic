package com.eimsound.eimusic.media

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
        setOnError {
            listener.onError()
        }
    }
    return mediaPlayer;
}

fun formatTime(seconds: Double): String {
    val totalSeconds = seconds.toLong()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

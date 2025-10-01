package com.eimsound.eimusic.media

import com.eimsound.eimusic.Duration
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

/**
 * 创建媒体播放器
 */
fun createMediaPlayer(source: String, listener: MediaPlayerListener): MediaPlayer {
    val media = Media(source)
    val mediaPlayer = MediaPlayer(media).apply {
        setOnReady {
            listener.onReady()
        }
        setOnEndOfMedia {
            listener.onAudioCompleted()
        }
        currentTimeProperty().addListener { _, _, newValue ->
            listener.timer(Duration(newValue.toSeconds().toLong()))
        }
        setOnError {
            listener.onError()
        }
        statusProperty().addListener { _, _, newValue ->
            when (newValue) {
                MediaPlayer.Status.STALLED -> listener.onLoading()
                else -> listener.onLoaded()
            }
        }

        // 添加缓冲进度监听
        bufferProgressTimeProperty().addListener { _, _, newProgress ->
            val totalDuration = totalDuration?.toSeconds()
            if (totalDuration != null && totalDuration > 0) {
                val progress = newProgress.toSeconds() / totalDuration
                Platform.runLater {
                    listener.onBufferProgress(progress.coerceIn(0.0, 1.0).toFloat())
                }
            }
        }
    }
    return mediaPlayer
}

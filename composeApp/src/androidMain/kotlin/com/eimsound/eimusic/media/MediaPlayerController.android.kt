package com.eimsound.eimusic.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.EIMusicApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android平台的媒体播放控制器实现
 * 使用ExoPlayer作为底层播放引擎
 */
actual class MediaPlayerController {
    private var exoPlayer: ExoPlayer? = null
    private var mediaPlayerListener: MediaPlayerListener? = null
    private var currentVolume: Float = 1.0f
    private var preparedSources: List<String> = emptyList()
    private var currentMediaItemIndex: Int = 0

    actual fun prepare(source: String, listener: MediaPlayerListener) {
        prepare(listOf(source), 0, listener)
    }
    
    fun prepare(sources: List<String>, startIndex: Int = 0, listener: MediaPlayerListener) {
        mediaPlayerListener = listener
        currentMediaItemIndex = startIndex
        
        // 如果播放列表相同，只是切换曲目
        if (preparedSources == sources && exoPlayer != null) {
            exoPlayer?.seekTo(startIndex, 0)
            return
        }
        
        // 释放之前的播放器实例
        release()
        preparedSources = sources
        
        // 创建新的ExoPlayer实例
        exoPlayer = ExoPlayer.Builder(EIMusicApplication.instance).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            mediaPlayerListener?.onReady()
                        }
                        Player.STATE_ENDED -> {
                            mediaPlayerListener?.onAudioCompleted()
                        }
                        Player.STATE_BUFFERING -> {
                            mediaPlayerListener?.onLoading()
                        }
                    }
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    super.onPlayWhenReadyChanged(playWhenReady, reason)
                    if (playWhenReady) {
                        mediaPlayerListener?.onLoaded()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    mediaPlayerListener?.onError()
                }

//                override fun onPlaybackProgressUpdate(positionMs: Long, bufferedPositionMs: Long, durationMs: Long) {
//                    super.onPlaybackProgressUpdate(positionMs, bufferedPositionMs, durationMs)
//                    if (durationMs != androidx.media3.common.C.TIME_UNSET) {
//                        val progress = if (durationMs > 0) bufferedPositionMs.toFloat() / durationMs else 0f
//                        mediaPlayerListener?.onBufferProgress(progress.coerceIn(0f, 1f))
//                    }
//                }
                
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    // 当媒体项切换时更新当前索引
                    exoPlayer?.let { player ->
                        this@MediaPlayerController.currentMediaItemIndex = player.currentMediaItemIndex
                    }
                }
            })
            
            // 创建媒体项列表
            val mediaItems = sources.map { source ->
                MediaItem.fromUri(Uri.parse(source))
            }
            
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
        }
    }

    actual fun start() {
        exoPlayer?.play()
    }

    actual fun pause() {
        exoPlayer?.pause()
    }

    actual fun stop() {
        exoPlayer?.stop()
    }

    actual val isPlaying: Boolean
        get() = exoPlayer?.isPlaying ?: false
    
    actual var volume: Double
        get() = exoPlayer?.volume?.toDouble() ?: currentVolume.toDouble()
        set(value) {
            currentVolume = value.toFloat()
            exoPlayer?.volume = currentVolume
        }
    
    actual val position: Duration?
        get() = exoPlayer?.currentPosition?.let { Duration(it / 1000) }
    
    actual val duration: Duration?
        get() = exoPlayer?.duration?.let { 
            if (it == androidx.media3.common.C.TIME_UNSET) null else Duration(it / 1000) 
        }

    actual fun release() {
        exoPlayer?.release()
        exoPlayer = null
        preparedSources = emptyList()
    }

    actual fun seek(seconds: Duration, seekOver: () -> Unit) {
        exoPlayer?.seekTo(seconds.seconds * 1000)
        seekOver()
    }

    actual var isMuted: Boolean
        get() = exoPlayer?.volume == 0f
        set(value) {
            exoPlayer?.volume = if (value) 0f else currentVolume
        }
}
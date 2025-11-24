package com.eimsound.eimusic.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.EIMusicApplication
import android.os.Handler
import android.os.Looper

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
    private var progressUpdateHandler: Handler? = null
    private var progressUpdateRunnable: Runnable? = null

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
        
        // 初始化进度更新处理器
        progressUpdateHandler = Handler(Looper.getMainLooper())
        progressUpdateRunnable = object : Runnable {
            override fun run() {
                exoPlayer?.let { player ->
                    // 报告播放进度
                    mediaPlayerListener?.timer(Duration(player.currentPosition / 1000))
                    
                    // 报告缓冲进度
                    val duration = player.duration
                    if (duration != androidx.media3.common.C.TIME_UNSET) {
                        val progress = if (duration > 0) player.bufferedPosition.toFloat() / duration else 0f
                        mediaPlayerListener?.onBufferProgress(progress.coerceIn(0f, 1f))
                    }
                }
                
                // 每100毫秒更新一次进度
                progressUpdateHandler?.postDelayed(this, 100)
            }
        }
        
        // 创建新的ExoPlayer实例
        exoPlayer = ExoPlayer.Builder(EIMusicApplication.instance).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            mediaPlayerListener?.onReady()
                            // 开始进度更新
                            progressUpdateHandler?.removeCallbacks(progressUpdateRunnable!!)
                            progressUpdateHandler?.post(progressUpdateRunnable!!)
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
                        // 开始进度更新
                        progressUpdateHandler?.removeCallbacks(progressUpdateRunnable!!)
                        progressUpdateHandler?.post(progressUpdateRunnable!!)
                    } else {
                        // 暂停时停止进度更新
                        progressUpdateHandler?.removeCallbacks(progressUpdateRunnable!!)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    mediaPlayerListener?.onError()
                    // 出错时停止进度更新
                    progressUpdateHandler?.removeCallbacks(progressUpdateRunnable!!)
                }
                
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
        // 停止进度更新
        progressUpdateHandler?.removeCallbacks(progressUpdateRunnable!!)
        progressUpdateHandler = null
        progressUpdateRunnable = null
        
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
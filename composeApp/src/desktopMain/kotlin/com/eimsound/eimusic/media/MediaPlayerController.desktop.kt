package com.eimsound.eimusic.media

import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.data.Storage
import com.eimsound.eimusic.settings.Settings
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import javafx.application.Platform

/**
 * 桌面平台的媒体播放控制器实现
 * 负责管理JavaFX MediaPlayer实例并提供统一的播放控制接口
 */
actual class MediaPlayerController {
    // 缓存最近使用的 MediaPlayer 实例，最多缓存 20 个
    private companion object {
        private const val MAX_CACHE_SIZE = 20
        private val mediaPlayerCache = ConcurrentHashMap<String, MediaPlayer>()
        private val accessOrder = mutableListOf<String>()
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentSource: String? = null
    private var currentPlayerListener: MediaPlayerListener? = null

    /**
     * 准备播放指定的媒体源
     * @param source 媒体源URL或文件路径
     * @param listener 媒体播放事件监听器
     */
    actual fun prepare(source: String, listener: MediaPlayerListener) {
        // 保存当前监听器
        currentPlayerListener = listener
        
        // 尝试使用缓存的播放器
        if (tryUseCachedPlayer(source, listener)) return
        
        // 清理并创建新播放器
        cleanupCurrentPlayer()
        ensureCacheSpace()
        createAndCacheNewPlayer(source, listener)
    }

    /**
     * 尝试使用已缓存的播放器
     * @return 如果成功使用缓存则返回true，否则返回false
     */
    private fun tryUseCachedPlayer(source: String, listener: MediaPlayerListener): Boolean {
        val cachedPlayer = mediaPlayerCache[source]
        if (cachedPlayer != null && cachedPlayer.status != MediaPlayer.Status.DISPOSED) {
            mediaPlayer = cachedPlayer
            currentSource = source
            
            // 更新访问顺序（LRU策略）
            accessOrder.remove(source)
            accessOrder.add(source)

            // 直接通知准备就绪
            listener.onReady()
            return true
        }
        return false
    }

    /**
     * 清理当前播放器
     */
    private fun cleanupCurrentPlayer() {
        mediaPlayer?.dispose()
    }

    /**
     * 确保缓存空间足够
     */
    private fun ensureCacheSpace() {
        if (mediaPlayerCache.size >= MAX_CACHE_SIZE) {
            val lruKey = accessOrder.firstOrNull()
            lruKey?.let { key ->
                mediaPlayerCache[key]?.dispose()
                mediaPlayerCache.remove(key)
                accessOrder.remove(key)
            }
        }
    }

    /**
     * 创建并缓存新的播放器
     */
    private fun createAndCacheNewPlayer(source: String, listener: MediaPlayerListener) {
        val newMediaPlayer = createMediaPlayer(source, listener)
        
        mediaPlayer = newMediaPlayer
        currentSource = source
        
        // 将新的 MediaPlayer 添加到缓存
        mediaPlayerCache[source] = newMediaPlayer
        accessOrder.add(source)
    }
    


    /**
     * 开始播放
     */
    actual fun start() {
        mediaPlayer?.play()
    }

    /**
     * 暂停播放
     */
    actual fun pause() {
        mediaPlayer?.pause()
    }

    /**
     * 停止播放
     */
    actual fun stop() {
        mediaPlayer?.stop()
    }

    /**
     * 获取当前播放位置
     */
    actual val position: Duration?
        get() = Duration(mediaPlayer?.currentTime?.toSeconds()?.toLong() ?: 0)

    /**
     * 获取媒体总时长
     */
    actual val duration: Duration?
        get() = Duration(mediaPlayer?.totalDuration?.toSeconds()?.toLong() ?: 0)

    /**
     * 检查是否正在播放
     */
    actual val isPlaying: Boolean
        get() = mediaPlayer?.status == MediaPlayer.Status.PLAYING

    /**
     * 获取或设置音量（0.0 - 1.0）
     */
    actual var volume: Double
        get() = mediaPlayer?.volume ?: 1.0
        set(value) {
            mediaPlayer?.volume = value
        }

    /**
     * 获取或设置是否静音
     */
    actual var isMuted: Boolean
        get() = mediaPlayer?.isMute ?: false
        set(value) {
            mediaPlayer?.isMute = value
        }

    /**
     * 跳转到指定位置
     * @param seconds 目标位置（秒）
     * @param seekOver 跳转完成回调
     */
    actual fun seek(seconds: Duration, seekOver: () -> Unit) {
        CompletableFuture.runAsync {
            mediaPlayer?.seek(javafx.util.Duration.seconds(seconds.seconds.toDouble()))
        }.thenRun {
            seekOver()
        }
    }

    /**
     * 释放当前媒体播放器资源
     */
    actual fun release() {
        // 不要从缓存中移除，保留以供下次使用
        mediaPlayer?.stop()
        mediaPlayer = null
        currentSource = null
        currentPlayerListener = null
    }
}
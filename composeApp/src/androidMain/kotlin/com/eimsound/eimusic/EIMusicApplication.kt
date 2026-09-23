package com.eimsound.eimusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class EIMusicApplication : Application() {
    companion object {
        lateinit var instance: EIMusicApplication
            private set
        const val FOREGROUND_SERVICE_CHANNEL_ID = "eimusic_foreground"
        const val NEW_MESSAGE_CHANNEL_ID = "eimusic_new_message"
    }

    override fun onCreate() {
        super<Application>.onCreate()
        instance = this
        AppStateProvider.start()
        initKoin() {
            androidContext(this@EIMusicApplication)
            modules(module {
                single<EIMusicMediaController> {
                    EIMusicMediaController().apply {
                        connect()
                    }
                }
            })
        }

        // 创建前台服务通知通道
        createNotificationChannel()

        createMessageNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FOREGROUND_SERVICE_CHANNEL_ID,
                "播放控制",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "用于显示正在播放的音乐和进度控制"
                // 关键：确保在锁屏上完全可见，这对灵动岛的初始化有帮助
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                // 音乐播放通常不需要桌面角标
                setShowBadge(false)
                // 禁用不必要的震动，防止系统因频繁切歌而误判为骚扰通知
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createMessageNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = NEW_MESSAGE_CHANNEL_ID
            val name = "新消息提醒"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()


            val channel = NotificationChannel(channelId, name, importance).apply {
                description = "用于接收即时聊天消息"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
                setSound(defaultSoundUri, attributes)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
package com.eimsound.eimusic.media

import com.eimsound.eimusic.viewmodel.NotificationState
import eimusic.composeapp.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.sound.sampled.AudioSystem

actual class NotificationService actual constructor(
    private val state: StateFlow<NotificationState>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var cachedSoundBytes: ByteArray? = null

    init {
        // 预加载音效（可选）：在服务启动时就异步读取到内存中
        scope.launch {
            loadSoundResource()
        }
    }

    private suspend fun loadSoundResource(): ByteArray? {
        return try {
            cachedSoundBytes ?: Res.readBytes("files/pop.wav").also {
                cachedSoundBytes = it
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual suspend fun playSound() {
        if (state.value.enabledNotification) {
            val bytes = loadSoundResource()
            if (bytes != null) {
                playNotificationSound(bytes)
            }
        }
    }

    fun playNotificationSound(res: ByteArray) {
        try {
            // 从 Resources 获取输入流
            val inputStream = java.io.ByteArrayInputStream(res)
            val bufferedIn = java.io.BufferedInputStream(inputStream)
            val audioStream = AudioSystem.getAudioInputStream(bufferedIn)

            val clip = AudioSystem.getClip()
            clip.open(audioStream)
            clip.start() // 开始播放

            // 可选：监听播放结束并关闭资源
            clip.addLineListener { event ->
                if (event.type == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    actual suspend fun notify(title: String, content: String) {
        if (state.value.enabledNotification && state.value.enabledNotificationSound) {
            val bytes = loadSoundResource()
            if (bytes != null) {
                playNotificationSound(bytes)
            }
        }
    }

}
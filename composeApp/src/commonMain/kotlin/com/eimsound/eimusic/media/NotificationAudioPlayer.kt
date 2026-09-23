package com.eimsound.eimusic.media

import com.eimsound.eimusic.viewmodel.NotificationState
import kotlinx.coroutines.flow.StateFlow


expect class NotificationService(state: StateFlow<NotificationState>) {
    suspend fun playSound()

    suspend fun notify(title: String, content: String)
}
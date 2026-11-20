package com.eimsound.eimusic.media

import com.eimsound.eimusic.viewmodel.NotificationState
import kotlinx.coroutines.flow.StateFlow

/** iOS currently has no app-level notification UI; keep the common service contract callable. */
actual class NotificationService actual constructor(state: StateFlow<NotificationState>) {
    actual suspend fun playSound() = Unit
    actual suspend fun notify(title: String, content: String) = Unit
}

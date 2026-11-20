package com.eimsound.eimusic.media

import com.eimsound.eimusic.viewmodel.NotificationState
import kotlinx.browser.document
import kotlinx.coroutines.flow.StateFlow
import org.w3c.dom.HTMLAudioElement

actual class NotificationService actual constructor(state: StateFlow<NotificationState>) {
    actual suspend fun playSound() {
        val audio = document.createElement("audio") as HTMLAudioElement
        audio.src = "pop.wav"
        audio.play()
    }

    actual suspend fun notify(title: String, content: String) {
        // Browser notifications require an explicit permission prompt. Keep the web UI
        // usable without prompting and let the in-app notification state remain visible.
    }
}

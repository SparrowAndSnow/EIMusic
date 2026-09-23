package com.eimsound.eimusic.media

import com.eimsound.eimusic.events.EventSource
import kotlinx.atomicfu.atomic


private object PlaybackContext {
    private val _eventSource = atomic<EventSource>(EventSource.Player)

    var eventSource: EventSource
        get() = _eventSource.value
        set(value) {
            _eventSource.value = value
        }
}
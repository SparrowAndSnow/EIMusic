package com.eimsound.eimusic.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局应用事件管理器
 * 用于在不同组件之间传递事件，实现解耦
 */
sealed interface EventBus {
    class PlaybackEventBus : EventBus {
        private val _event = MutableSharedFlow<PlaybackEvent>(replay = 0, extraBufferCapacity = 64)
        val event: SharedFlow<PlaybackEvent> = _event.asSharedFlow()
        fun send(event: PlaybackEvent): Boolean {
            return _event.tryEmit(event)
        }
    }

    class PlayingListEventBus : EventBus {
        private val _event = MutableSharedFlow<PlayingListEvent>(replay = 0, extraBufferCapacity = 64)
        val event: SharedFlow<PlayingListEvent> = _event.asSharedFlow()
        fun send(event: PlayingListEvent): Boolean {
            return _event.tryEmit(event)
        }
    }
}
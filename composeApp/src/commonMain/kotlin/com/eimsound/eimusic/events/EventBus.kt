package com.eimsound.eimusic.events

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 全局应用事件管理器
 * 用于在不同组件之间传递事件，实现解耦
 */
sealed interface EventBus<T> {
    fun send(event: T): Boolean
    suspend fun receive(listener: (T) -> Unit)

    object PlaybackEventBus : EventBus<PlaybackEvent> {
        private val _event = MutableSharedFlow<PlaybackEvent>(replay = 0, extraBufferCapacity = 64)

        override fun send(event: PlaybackEvent): Boolean =
            _event.tryEmit(event)
        override suspend fun receive(listener: (PlaybackEvent) -> Unit) =
            _event.collect(listener)
    }

    object PlayingListEventBus : EventBus<PlayingListEvent> {
        private val _event = MutableSharedFlow<PlayingListEvent>(replay = 0, extraBufferCapacity = 64)

        override fun send(event: PlayingListEvent): Boolean =
            _event.tryEmit(event)

        override suspend fun receive(listener: (PlayingListEvent) -> Unit) =
            _event.collect(listener)
    }
    
    object TrackListEventBus : EventBus<TrackListEvent> {
        private val _event = MutableSharedFlow<TrackListEvent>(replay = 0, extraBufferCapacity = 64)

        override fun send(event: TrackListEvent): Boolean =
            _event.tryEmit(event)

        override suspend fun receive(listener: (TrackListEvent) -> Unit) =
            _event.collect(listener)
    }
}
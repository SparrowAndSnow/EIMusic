package com.eimsound.eimusic.events

import com.eimsound.eimusic.media.PlayMode

sealed interface PlaybackEvent : Event<PlaybackEvent> {
    data class Next(val playMode: PlayMode) : PlaybackEvent
    data class Previous(val playMode: PlayMode) : PlaybackEvent
    data class PlaybackModeChanged(val playMode: PlayMode) : PlaybackEvent
    data class TogglePlaybackPause(val isPlaying: Boolean) : PlaybackEvent
}
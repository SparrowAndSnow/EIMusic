package com.eimsound.eimusic.events

import com.eimsound.eimusic.media.PlayMode
import com.eimsound.eimusic.music.Track

// 播放事件类型
sealed interface PlaybackEvent : Event<PlaybackEvent> {
    data class Play(val track: Track) : PlaybackEvent
    data class Next(val playMode: PlayMode) : PlaybackEvent
    data class Previous(val playMode: PlayMode) : PlaybackEvent
    data class PlaybackModeChanged(val playMode: PlayMode) : PlaybackEvent
    data class TogglePlaybackPause(val isPlaying: Boolean) : PlaybackEvent
}
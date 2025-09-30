package com.eimsound.eimusic.events

import com.eimsound.eimusic.music.Track

// 播放事件类型
sealed interface PlayingListEvent : Event<PlayingListEvent> {
    data class TrackChanged(val track: Track) : PlayingListEvent
}
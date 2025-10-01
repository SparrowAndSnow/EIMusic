package com.eimsound.eimusic.events

import com.eimsound.eimusic.music.Track

sealed interface PlayingListEvent : Event<PlayingListEvent> {
    data class TrackChanged(val track: Track) : PlayingListEvent
}
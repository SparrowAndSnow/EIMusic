package com.eimsound.eimusic.events

import com.eimsound.eimusic.music.Track

sealed interface TrackListEvent : Event<TrackListEvent> {
    data class TrackSelected(val track: Track) : TrackListEvent
    data class AddedToQueue(val track: Track) : TrackListEvent
    data class Error(val exception: Exception) : TrackListEvent
    data class PlayTrackList(val tracks: List<Track>, val track: Track) : TrackListEvent
}
package com.eimsound.eimusic.events

import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.Track

sealed interface PlayingListEvent : Event<PlayingListEvent> {
    data class TrackListChanged(
        val tracks: List<Track>,
        val track: Track? = null,
        override val source: EventSource = EventSource.User,
        override val targets: Set<EventSource> = setOf(EventSource.Remote, EventSource.Player)
    ) : PlayingListEvent{
        override fun toString(): String {
            return "播放列表已更改"
        }
    }

    data class TrackAdded(
        val track: Track,
        val strategy: AddTrackStrategy,
        override val source: EventSource = EventSource.User,
        override val targets: Set<EventSource> = setOf(EventSource.Remote, EventSource.Player)
    ) : PlayingListEvent{
        override fun toString(): String {
            return when (strategy) {
                is AddTrackStrategy.AddToLast -> "添加到播放列表 ${track.name}"
                is AddTrackStrategy.AddToNext -> "下一首播放 ${track.name}"
                is AddTrackStrategy.InsertToQueue -> "插入到播放列表 ${track.name}"
            }
        }
    }

    data class TrackRemoved(
        val track: Track,
        override val source: EventSource = EventSource.User,
        override val targets: Set<EventSource> = setOf(EventSource.Remote, EventSource.Player)
    ) : PlayingListEvent{
        override fun toString(): String {
            return "已删除歌曲 ${track.name}"
        }
    }

    data class TrackMoved(
        val from: Int,
        val to: Int,
        override val source: EventSource = EventSource.User,
        override val targets: Set<EventSource> = setOf(EventSource.Remote, EventSource.Player),
    ) : PlayingListEvent {
        override fun toString(): String = "移动歌曲 $from -> $to"
    }
}
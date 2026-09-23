package com.eimsound.eimusic.events

import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.Track

sealed interface PlaybackEvent : Event<PlaybackEvent> {
    data class PlayStateChanged(
        val isPlaying: Boolean,
        val position: Duration? = null,
        override val source: EventSource = EventSource.User,
        override val targets: Set<EventSource> = setOf(EventSource.Remote, EventSource.Player)
    ) : PlaybackEvent {
        override fun toString(): String {
            return if (isPlaying) {
                "继续播放"
            } else {
                "暂停播放"
            }
        }
    }

    data class Next(
        val isAudioCompleted: Boolean = false,
        val track: Track? = null,
        override val source: EventSource = EventSource.User,
        override val targets: Set<EventSource> = setOf(EventSource.Remote, EventSource.Player)
    ) : PlaybackEvent {
        override fun toString(): String {
            return "下一首"
        }
    }

    data class Previous(
        val track: Track? = null,
        override val source: EventSource = EventSource.User,
        override val targets: Set<EventSource> = setOf(EventSource.Remote, EventSource.Player)
    ) : PlaybackEvent {
        override fun toString(): String {
            return "上一首"
        }
    }

    data class PlayModeChanged(
        val playMode: PlayMode,
        override val source: EventSource = EventSource.User,
        override val targets: Set<EventSource> = setOf(EventSource.Remote, EventSource.Player)
    ) : PlaybackEvent {
        override fun toString(): String {
            return when (playMode) {
                is PlayMode.Loop -> "顺序播放"
                is PlayMode.Shuffle -> "随机播放"
                is PlayMode.RepeatOne -> "单曲循环"
            }
        }
    }

    data class Seek(
        val position: Duration,
        val seekOver: () -> Unit = {},
        override val source: EventSource = EventSource.User,
        override val targets: Set<EventSource> = setOf(EventSource.Remote, EventSource.Player)
    ) : PlaybackEvent {
        override fun toString(): String {
            return "跳转到 $position"
        }
    }

    data class TrackChanged(
        val track: Track,
        val isAudioCompleted: Boolean = true,
        val autoPlay: Boolean = true,
        val prepareOver: () -> Unit = {},
        override val source: EventSource = EventSource.User,
        override val targets: Set<EventSource> = setOf(EventSource.Remote, EventSource.Player)
    ) : PlaybackEvent {
        override fun toString(): String {
            return "开始播放 ${track.name}"
        }
    }
}
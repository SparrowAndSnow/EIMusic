package com.eimsound.eimusic.together

import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.Track
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface PlayControlEvent {
    @Serializable
    @SerialName("play")
    data class Play(val position: Long) : PlayControlEvent {
        override fun toString(): String {
            return "继续播放"
        }
    }

    @Serializable
    @SerialName("pause")
    data class Pause(val position: Long) : PlayControlEvent {
        override fun toString(): String {
            return "暂停播放"
        }
    }

    @Serializable
    @SerialName("seek")
    data class Seek(val position: Long) : PlayControlEvent {
        override fun toString(): String {
            return "跳转到 ${Duration(position)}"
        }
    }

    @Serializable
    @SerialName("queue_change")
    data class QueueChange(
        val kind: QueueChangeKind,
        val track: Track? = null,
        val strategy: AddTrackStrategy? = null,
        val index: Int = -1,
        val toIndex: Int = -1,
        val baseRevision: Long? = null,
    ) : PlayControlEvent {
        override fun toString(): String = when (kind) {
            QueueChangeKind.ADD -> "添加歌曲 ${track?.name}"
            QueueChangeKind.REMOVE -> "删除歌曲 ${track?.name}"
            QueueChangeKind.MOVE -> "移动歌曲 $index -> $toIndex"
        }
    }

    @Serializable
    @SerialName("change_track")
    data class ChangeTrack(val track: Track, val isAudioCompleted: Boolean = false) : PlayControlEvent {
        override fun toString(): String {
            return "开始播放 ${track.name}"
        }
    }

    @Serializable
    enum class QueueChangeKind {
        ADD, REMOVE, MOVE
    }

    @Serializable
    @SerialName("add_to_queue")
    data class AddToQueue(val track: Track, val strategy: AddTrackStrategy) : PlayControlEvent {
        override fun toString(): String {
            return when (strategy) {
                is AddTrackStrategy.AddToLast -> "添加到播放列表 ${track.name}"
                is AddTrackStrategy.AddToNext -> "下一首播放 ${track.name}"
                is AddTrackStrategy.InsertToQueue -> "插入到播放列表 ${track.name}"
            }
        }
    }

    @Serializable
    @SerialName("remove_from_queue")
    data class RemoveFromQueue(val track: Track) : PlayControlEvent {
        override fun toString(): String {
            return "已从播放列表删除 ${track.name}"
        }
    }

    @Serializable
    @SerialName("change_track_list")
    data class ChangeTrackList(
        val trackList: List<Track>,
        val play: Track? = null,
        val baseRevision: Long? = null,
    ) : PlayControlEvent {
        override fun toString(): String {
            return "播放列表已更新"
        }
    }

    @Serializable
    @SerialName("next")
    data class Next(val track: Track? = null) : PlayControlEvent {
        override fun toString(): String {
            return "下一首"
        }
    }

    @Serializable
    @SerialName("previous")
    data class Previous(val track: Track? = null) : PlayControlEvent {
        override fun toString(): String {
            return "上一首"
        }
    }

    @Serializable
    @SerialName("stop")
    object Stop : PlayControlEvent {
        override fun toString(): String {
            return "停止播放"
        }
    }

    @Serializable
    @SerialName("play_mode_changed")
    data class PlayModeChanged(val mode: PlayMode) : PlayControlEvent {
        override fun toString(): String {
            return when (mode) {
                is PlayMode.Loop -> "顺序播放"
                is PlayMode.Shuffle -> "随机播放"
                is PlayMode.RepeatOne -> "单曲循环"
            }
        }
    }

}
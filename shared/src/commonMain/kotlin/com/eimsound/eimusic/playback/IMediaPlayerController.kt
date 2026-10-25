package com.eimsound.eimusic.playback

import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IMediaPlayerController {

    val state: StateFlow<PlayerState>

    val event: SharedFlow<PlaybackEvent>

    fun queue(medias: List<Track>, prepareOver: () -> Unit = {})

    fun play(media: Track, origin: PlaybackOrigin = PlaybackOrigin.LocalUi)

    fun next(
        isAudioCompleted: Boolean = false,
        origin: PlaybackOrigin = PlaybackOrigin.LocalUi,
    )

    fun addTrack(media: Track, index: Int)

    fun removeTrack(index: Int)

    /** 只重排队列，不停止/重载当前曲目。 */
    fun updateQueue(tracks: List<Track>) {}

    /** 在队列内移动曲目；平台不支持时默认忽略。 */
    fun moveTrack(from: Int, to: Int) {}

    fun nextTrack(): Track?

    fun previous(origin: PlaybackOrigin = PlaybackOrigin.LocalUi)

    fun previousTrack(): Track?

    fun playState(state: PlayState, origin: PlaybackOrigin = PlaybackOrigin.LocalUi)

    fun playMode(mode: PlayMode, origin: PlaybackOrigin = PlaybackOrigin.LocalUi)

    fun volume(volume: Float, isMuted: Boolean = false)

    fun seek(
        millis: Long,
        seekOver: () -> Unit = {},
        origin: PlaybackOrigin = PlaybackOrigin.LocalUi,
    )

    fun release()

    fun sync(syncState: SyncState)

    /** 恢复队列与当前曲目（暂停，不自动播放）。 */
    fun restore(queue: List<Track>, currentId: TrackId?) {}
}
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

    fun play(media: Track)

    fun next(isAudioCompleted: Boolean = false)

    fun addTrack(media: Track, index: Int)

    fun removeTrack(index: Int)

    /** 在队列内移动曲目；平台不支持时默认忽略。 */
    fun moveTrack(from: Int, to: Int) {}

    fun nextTrack(): Track?

    fun previous()

    fun previousTrack(): Track?

    fun playState(state: PlayState)

    fun playMode(mode: PlayMode)

    fun volume(volume: Float, isMuted: Boolean = false)

    fun seek(millis: Long, seekOver: () -> Unit = {})

    fun release()

    fun sync(syncState: SyncState)

    /** 恢复队列与当前曲目（暂停，不自动播放）。 */
    fun restore(queue: List<Track>, currentId: TrackId?) {}
}
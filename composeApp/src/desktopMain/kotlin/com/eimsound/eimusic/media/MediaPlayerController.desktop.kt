package com.eimsound.eimusic.media

import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.eimsound.eimusic.playback.IMediaPlayerController
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 桌面平台的媒体播放控制器实现
 * 负责管理JavaFX MediaPlayer实例并提供统一的播放控制接口
 */
actual class MediaPlayerController actual constructor(
    override val trackUriResolverFactory: TrackUriResolverFactory
) : JavaStreamPlayerController(trackUriResolverFactory), IMediaPlayerController {
    actual override val state: StateFlow<PlayerState> = super.state
    actual override val event: SharedFlow<PlaybackEvent> = super.event
    actual override fun queue(medias: List<Track>, prepareOver: () -> Unit) = super.queue(medias, prepareOver)

    actual override fun play(media: Track) = super.play(media)

    actual override fun next(isAudioCompleted: Boolean) = super.next(isAudioCompleted)

    actual override fun addTrack(media: Track, index: Int) = super.addTrack(media, index)

    actual override fun removeTrack(index: Int) = super.removeTrack(index)

    actual override fun nextTrack(): Track? = super.nextTrack()

    actual override fun previous() = super.previous()

    actual override fun previousTrack(): Track? = super.previousTrack()

    actual override fun playState(state: PlayState) = super.playState(state)

    actual override fun playMode(mode: PlayMode) = super.playMode(mode)

    actual override fun volume(volume: Float, isMuted: Boolean) = super.volume(volume, isMuted)

    actual override fun seek(millis: Long, seekOver: () -> Unit) = super.seek(millis, seekOver)

    actual override fun release() = super.release()

    actual override fun sync(syncState: SyncState) = super.sync(syncState)

}

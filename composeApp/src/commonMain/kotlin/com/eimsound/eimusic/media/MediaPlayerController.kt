package com.eimsound.eimusic.media

import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.eimsound.eimusic.playback.IMediaPlayerController
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow


expect class MediaPlayerController(trackUriResolverFactory: TrackUriResolverFactory): IMediaPlayerController {

    override val state: StateFlow<PlayerState>

    override val event: SharedFlow<PlaybackEvent>

    override fun queue(medias: List<Track>, prepareOver: () -> Unit)

    override fun play(media: Track)

    override fun next(isAudioCompleted: Boolean)

    override fun addTrack(media: Track, index: Int)

    override fun removeTrack(index: Int)

    override fun nextTrack(): Track?

    override fun previous()

    override fun previousTrack(): Track?

    override fun playState(state: PlayState)

    override fun playMode(mode: PlayMode)

    override fun volume(volume: Float, isMuted: Boolean )

    override fun seek(millis: Long, seekOver: () -> Unit)

    override fun release()

    override fun sync(syncState: SyncState)
}


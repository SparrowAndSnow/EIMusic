package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.change
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.playback.PlaybackStore
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.TogetherSyncManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** composeApp 的薄壳：保留 EventBus 桥接 + AndroidX 生命周期。 */
class PlayerViewModel(
    private val store: PlaybackStore,
    private val syncManager: TogetherSyncManager,
    private val controller: IMediaPlayerController,
) : ViewModel() {

    val state = store.state
    val event = controller.event

    init {
        viewModelScope.launch {
            EventBus.PlaybackEventBus.receive { event ->
                if (syncManager.shouldIgnoreEventSource(event.source)) return@receive
                if (event.source != EventSource.System) handlePlaybackEvent(event)
            }
        }

        viewModelScope.launch {
            EventBus.PlayingListEventBus.receive { event ->
                // 本地 UI 已直接调用 PlaybackStore；这里只应用远端同步事件。
                if (event.source == EventSource.Remote) handlePlayingListEvent(event)
            }
        }

        event.onEach { EventBus.PlaybackEventBus.send(it) }.launchIn(viewModelScope)
    }

    private fun handlePlaybackEvent(event: PlaybackEvent) {
        when (event) {
            is PlaybackEvent.PlayStateChanged -> handlePlayStateChanged(event)
            is PlaybackEvent.PlayModeChanged -> onPlayModeChanged(event.playMode)
            is PlaybackEvent.Seek -> seek(event.position, event.seekOver)
            is PlaybackEvent.Next -> next()
            is PlaybackEvent.Previous -> previous()
            is PlaybackEvent.TrackChanged -> handleTrackChanged(event)
        }
    }

    private fun handlePlayStateChanged(event: PlaybackEvent.PlayStateChanged) {
        when (event.source) {
            EventSource.Player, EventSource.User -> isPlaying(event.isPlaying)
            EventSource.Remote -> seek(event.position ?: Duration(0)) { isPlaying(event.isPlaying) }
            else -> {}
        }
    }

    private fun handleTrackChanged(event: PlaybackEvent.TrackChanged) {
        if (event.source != EventSource.Player) changeTrack(event.track)
    }

    private fun handlePlayingListEvent(event: PlayingListEvent) {
        when (event) {
            is PlayingListEvent.TrackListChanged ->
                store.applyTrackListChanged(event.tracks, event.track, event.source)
            is PlayingListEvent.TrackAdded -> store.applyTrackAdded(event.track, event.strategy)
            is PlayingListEvent.TrackRemoved -> store.applyTrackRemoved(event.track)
            is PlayingListEvent.TrackMoved -> controller.moveTrack(event.from, event.to)
        }
    }

    fun sendTrackChanged(track: Track, isAudioCompleted: Boolean = false, source: EventSource = EventSource.User) {
        EventBus.PlaybackEventBus.send(
            PlaybackEvent.TrackChanged(track, isAudioCompleted = isAudioCompleted, autoPlay = true, source = source),
        )
    }

    private fun previous() = controller.previous()
    private fun next() = controller.next()

    fun onPlayModeChanged(value: PlayMode) = store.setPlayMode(value)

    fun sendPlayModeChanged() {
        val value = state.value.playMode.change { state.value.queue.size }
        EventBus.PlaybackEventBus.send(PlaybackEvent.PlayModeChanged(value))
    }

    fun onVolumeChanged(value: Float) = store.setVolume(value)
    fun onIsMuteChanged(value: Boolean) = store.setMute(value)

    fun onPlayingTrackChanged(tracks: List<Track>, track: Track? = null, source: EventSource = EventSource.User) =
        store.applyTrackListChanged(tracks, track, source)

    fun onPlayingTrackAdded(track: Track, strategy: AddTrackStrategy) = store.applyTrackAdded(track, strategy)

    fun onPlayingTrackRemoved(track: Track) = store.applyTrackRemoved(track)

    fun changeTrack(track: Track) {
        if (track.id == state.value.current?.id) return
        controller.play(track)
    }

    fun sendNext(isAudioCompleted: Boolean = false) {
        EventBus.PlaybackEventBus.send(PlaybackEvent.Next(isAudioCompleted, controller.nextTrack()))
    }

    fun sendPrevious() {
        EventBus.PlaybackEventBus.send(PlaybackEvent.Previous(controller.previousTrack()))
    }

    fun isPlaying(value: Boolean) {
        controller.playState(if (value) PlayState.PLAYING else PlayState.PAUSED)
    }

    fun sendIsPlaying(value: Boolean) {
        EventBus.PlaybackEventBus.send(PlaybackEvent.PlayStateChanged(value, Duration(state.value.position)))
    }

    fun seek(value: Duration, seekOver: () -> Unit = {}) {
        controller.seek(value.milliseconds) { seekOver() }
    }

    fun sendSeek(position: Duration, seekOver: () -> Unit = {}) {
        EventBus.PlaybackEventBus.send(PlaybackEvent.Seek(position, seekOver))
    }
}

package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.db.DbLibraryRepository
import com.eimsound.eimusic.db.DbSessionRepository
import com.eimsound.eimusic.db.toDbTrack
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.change
import com.eimsound.eimusic.music.name
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.TogetherSyncManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val storage: IStorage,
    private val syncManager: TogetherSyncManager,
    private val controller: IMediaPlayerController,
    private val library: DbLibraryRepository,
    private val session: DbSessionRepository,
) : ViewModel() {
    init {
        viewModelScope.launch {
            EventBus.PlaybackEventBus.receive { event ->
                if (syncManager.shouldIgnoreEventSource(event.source)) return@receive
                if (event.source != EventSource.System) handlePlaybackEvent(event)
            }
        }
        
        viewModelScope.launch {
            EventBus.PlayingListEventBus.receive { event ->
                if (syncManager.shouldIgnoreEventSource(event.source)) return@receive
                handlePlayingListEvent(event)
            }
        }
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
            EventSource.Remote -> {
                seek(event.position ?: Duration(0)) {
                    isPlaying(event.isPlaying)
                }
            }
            else -> {}
        }
    }

    private fun handleTrackChanged(event: PlaybackEvent.TrackChanged) {
        if (event.source != EventSource.Player) {
            changeTrack(event.track)
        }
    }

    private fun handlePlayingListEvent(event: PlayingListEvent) {
        when (event) {
            is PlayingListEvent.TrackListChanged -> {
                onPlayingTrackChanged(event.tracks, event.track, event.source)
            }
            is PlayingListEvent.TrackAdded -> {
                onPlayingTrackAdded(event.track, event.strategy)
            }
            is PlayingListEvent.TrackRemoved -> {
                onPlayingTrackRemoved(event.track)
            }
            is PlayingListEvent.TrackMoved -> {
                controller.moveTrack(event.from, event.to)
            }
        }
    }


    val state = controller.state
    val event = controller.event

    init {
        initializeController()
        setupEventForwarding()
    }

    private fun initializeController() {
        controller.playMode(
            PlayMode.of(storage.get(Settings::playMode, PlayMode.Loop.name))
        )
        controller.volume(
            storage.get(Settings::volume, 1.0F),
            storage.get(Settings::isMuted, false)
        )
    }

    private fun setupEventForwarding() {
        event.onEach {
            EventBus.PlaybackEventBus.send(it)
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            var lastTrackId: String? = null
            state.map { it.queue to it.current }
                .distinctUntilChanged()
                .collect { (queue, current) ->
                    session.savePlayback(
                        queue = queue.map { it.toDbTrack() },
                        currentTrackId = current?.id?.toString(),
                    )
                    if (current != null && current.id.toString() != lastTrackId) {
                        lastTrackId = current.id.toString()
                        library.recordRecent(current.toDbTrack())
                    }
                }
        }
    }

    fun sendTrackChanged(track: Track, isAudioCompleted: Boolean = false, source: EventSource = EventSource.User) {
        EventBus.PlaybackEventBus.send(
            PlaybackEvent.TrackChanged(
                track,
                isAudioCompleted = isAudioCompleted,
                autoPlay = true,
                source = source,
            )
        )
    }

    private fun previous() {
        controller.previous()
    }

    private fun next() {
        controller.next()
    }

    fun onPlayModeChanged(value: PlayMode) {
        controller.playMode(value)
        storage.save(Settings::playMode, value.name)
    }

    fun sendPlayModeChanged() {
        val playMode = state.value.playMode
        val value = playMode.change { state.value.queue.size }
        EventBus.PlaybackEventBus.send(PlaybackEvent.PlayModeChanged(value))
    }

    fun onVolumeChanged(value: Float) {
        controller.volume(value)
        storage.save(Settings::volume, value)
    }

    fun onIsMuteChanged(value: Boolean) {
        controller.volume(state.value.volume, value)
        storage.save(Settings::isMuted, value)
    }

    fun onPlayingTrackChanged(tracks: List<Track>, track: Track? = null, source: EventSource = EventSource.User) {
        controller.queue(tracks) {
            track?.let {
                sendTrackChanged(track, source = source)
            }
        }
    }

    fun onPlayingTrackAdded(track: Track, strategy: AddTrackStrategy) {
        val index = calculateTrackIndex(strategy)
        controller.addTrack(track, index)
    }

    private fun calculateTrackIndex(strategy: AddTrackStrategy): Int {
        return when (strategy) {
            is AddTrackStrategy.AddToLast -> state.value.queue.size
            is AddTrackStrategy.AddToNext -> state.value.index + 1
            is AddTrackStrategy.InsertToQueue -> strategy.index
        }
    }

    fun onPlayingTrackRemoved(track: Track) {
        val index = state.value.queue.indexOfFirst { track.id == it.id }
        if (index == -1) return
        controller.removeTrack(index)
    }

    fun changeTrack(track: Track) {
        if (track.id == state.value.current?.id) return
        controller.play(track)
    }

    fun sendNext(isAudioCompleted: Boolean = false) {
        val nextTrack = controller.nextTrack()
        EventBus.PlaybackEventBus.send(PlaybackEvent.Next(isAudioCompleted, nextTrack))
    }

    fun sendPrevious() {
        val previousTrack = controller.previousTrack()
        EventBus.PlaybackEventBus.send(PlaybackEvent.Previous(previousTrack))
    }

    fun isPlaying(value: Boolean) {
        controller.playState(if (value) PlayState.PLAYING else PlayState.PAUSED)
    }

    fun sendIsPlaying(value: Boolean) {
        EventBus.PlaybackEventBus.send(PlaybackEvent.PlayStateChanged(value, Duration(state.value.position)))
    }


    fun seek(value: Duration, seekOver: () -> Unit = {}) {
        controller.seek(value.milliseconds) {
            seekOver()
        }
    }

    fun sendSeek(position: Duration, seekOver: () -> Unit = {}) {
        EventBus.PlaybackEventBus.send(PlaybackEvent.Seek(position, seekOver))
    }
}
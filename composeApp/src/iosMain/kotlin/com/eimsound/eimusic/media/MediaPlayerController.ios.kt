package com.eimsound.eimusic.media

import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId
import com.eimsound.eimusic.music.nextTrackCandidate
import com.eimsound.eimusic.music.previousTrackCandidate
import com.eimsound.eimusic.music.withTrackAdded
import com.eimsound.eimusic.music.withTrackRemoved
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.playback.PlaybackOrigin
import com.eimsound.eimusic.playback.toEventSource
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL

/** AVPlayer-backed iOS implementation; state remains the source of truth for queue operations. */
@OptIn(ExperimentalForeignApi::class)
actual class MediaPlayerController actual constructor(
    private val trackUriResolverFactory: TrackUriResolverFactory,
) : IMediaPlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val player = AVPlayer()
    private val _state = MutableStateFlow(PlayerState())
    actual override val state: StateFlow<PlayerState> = _state.asStateFlow()
    private val _event = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 16)
    actual override val event: SharedFlow<PlaybackEvent> = _event.asSharedFlow()
    private var resolveJob: Job? = null
    private var playGeneration = 0L
    private var endObserver: Any? = null

    init {
        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification,
            null,
            NSOperationQueue.mainQueue,
        ) { notification ->
            if (notification?.`object` == player.currentItem) {
                next(isAudioCompleted = true, origin = PlaybackOrigin.InternalAutoAdvance)
            }
        }
        scope.launch {
            while (true) {
                delay(500)
                val seconds = CMTimeGetSeconds(player.currentTime())
                val duration = player.currentItem?.duration?.let { CMTimeGetSeconds(it) }
                if (seconds.isFinite() && seconds >= 0) {
                    _state.update {
                        it.copy(
                            position = (seconds * 1000).toLong(),
                            duration = duration?.takeIf { value -> value.isFinite() && value >= 0 }
                                ?.let { value -> (value * 1000).toLong() } ?: it.duration,
                        )
                    }
                }
            }
        }
    }

    actual override fun queue(medias: List<Track>, prepareOver: () -> Unit) {
        ++playGeneration
        resolveJob?.cancel()
        _state.update { it.copy(queue = medias, current = null, index = 0, position = 0, duration = 0, isLoading = false, playState = PlayState.STOPPED) }
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        prepareOver()
    }

    actual override fun play(media: Track, origin: PlaybackOrigin) {
        val playOrigin = origin
        val trackChanged = _state.value.current?.id != media.id
        val generation = ++playGeneration
        resolveJob?.cancel()
        resolveJob = scope.launch {
            val uri = runCatching { trackUriResolverFactory.get(media.source).resolve(media) }.getOrNull()
            if (generation != playGeneration) return@launch
            val url = uri?.let {
                if (it.contains("://")) NSURL.URLWithString(it) else NSURL.fileURLWithPath(it)
            } ?: return@launch
            val list = _state.value.currentList
            val index = list.indexOfFirst { it.id == media.id }
            _state.update { it.copy(current = media, index = index.coerceAtLeast(0), isLoading = true, position = 0) }
            player.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = url))
            player.play()
            _state.update { it.copy(isLoading = false, playState = PlayState.PLAYING) }
            if (trackChanged) _event.emit(PlaybackEvent.TrackChanged(media, source = playOrigin.toEventSource()))
        }
    }

    actual override fun next(isAudioCompleted: Boolean, origin: PlaybackOrigin) {
        if (_state.value.playMode is PlayMode.RepeatOne) {
            seek(0, {}, origin)
            return
        }
        val target = nextTrack()
        _event.tryEmit(PlaybackEvent.Next(isAudioCompleted, target, origin.controllerEventSource()))
        target?.let { play(it, origin) }
    }

    actual override fun previous(origin: PlaybackOrigin) {
        if (_state.value.playMode is PlayMode.RepeatOne) {
            seek(0, {}, origin)
            return
        }
        val target = previousTrack()
        _event.tryEmit(PlaybackEvent.Previous(target, origin.controllerEventSource()))
        target?.let { play(it, origin) }
    }

    actual override fun playState(state: PlayState, origin: PlaybackOrigin) {
        val changed = _state.value.playState != state
        when (state) {
            PlayState.PLAYING -> {
                val current = _state.value.current
                if (player.currentItem == null && current != null) {
                    play(current, origin)
                } else {
                    player.play()
                    _state.update { it.copy(playState = state) }
                }
            }
            PlayState.PAUSED -> { player.pause(); _state.update { it.copy(playState = state) } }
            PlayState.STOPPED -> { player.pause(); player.seekToTime(CMTimeMake(0, 1000)); _state.update { it.copy(playState = state, position = 0) } }
        }
        if (changed) _event.tryEmit(PlaybackEvent.PlayStateChanged(state == PlayState.PLAYING, Duration(_state.value.position), origin.toEventSource()))
    }

    actual override fun playMode(mode: PlayMode, origin: PlaybackOrigin) {
        if (_state.value.playMode != mode) _event.tryEmit(PlaybackEvent.PlayModeChanged(mode, origin.toEventSource()))
        _state.update { it.copy(playMode = mode) }
    }

    actual override fun volume(volume: Float, isMuted: Boolean) {
        val normalized = volume.coerceIn(0f, 1f)
        player.volume = if (isMuted) 0f else normalized
        _state.update { it.copy(volume = normalized, isMute = isMuted) }
    }

    actual override fun seek(millis: Long, seekOver: () -> Unit, origin: PlaybackOrigin) {
        val target = millis.coerceAtLeast(0)
        val changed = _state.value.position != target
        _state.update { it.copy(position = target) }
        player.seekToTime(CMTimeMake(target, 1000)) { finished ->
            if (finished) seekOver()
        }
        if (changed) _event.tryEmit(PlaybackEvent.Seek(Duration(target), seekOver, origin.toEventSource()))
    }

    actual override fun release() {
        ++playGeneration
        resolveJob?.cancel()
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        player.pause()
        scope.cancel()
    }

    override fun restore(queue: List<Track>, currentId: TrackId?) {
        val index = queue.indexOfFirst { it.id == currentId }
        val current = queue.getOrNull(index)
        ++playGeneration
        resolveJob?.cancel()
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        _state.update {
            it.copy(
                queue = queue,
                current = current,
                index = index.coerceAtLeast(0),
                position = 0,
                duration = 0,
                isLoading = false,
                playState = if (current == null) PlayState.STOPPED else PlayState.PAUSED,
            )
        }
    }

    actual override fun nextTrack(): Track? {
        return _state.value.nextTrackCandidate()
    }

    actual override fun previousTrack(): Track? {
        return _state.value.previousTrackCandidate()
    }

    actual override fun sync(syncState: SyncState) {
        syncState.trackList?.let { tracks -> updateQueue(tracks) }
        syncState.playMode?.let { playMode(it, PlaybackOrigin.RemoteSync) }
        val track = syncState.track
        if (track != null) {
            val trackChanged = _state.value.current?.id != track.id
            val generation = ++playGeneration
            resolveJob?.cancel()
            resolveJob = scope.launch {
                val uri = runCatching { trackUriResolverFactory.get(track.source).resolve(track) }.getOrNull()
                if (generation != playGeneration) return@launch
                val url = uri?.let { if (it.contains("://")) NSURL.URLWithString(it) else NSURL.fileURLWithPath(it) } ?: return@launch
                player.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = url))
                _state.update { it.copy(current = track, index = it.currentList.indexOfFirst { item -> item.id == track.id }.coerceAtLeast(0), isLoading = false, position = 0) }
                val position = syncState.position ?: 0L
                player.seekToTime(CMTimeMake(position.coerceAtLeast(0), 1000)) { completed ->
                    if (!completed || generation != playGeneration) return@seekToTime
                    _state.update { it.copy(position = position.coerceAtLeast(0)) }
                    syncState.playState?.let { playState(it, PlaybackOrigin.RemoteSync) }
                        ?: player.play()
                }
                if (trackChanged) _event.emit(PlaybackEvent.TrackChanged(track, source = PlaybackOrigin.RemoteSync.toEventSource()))
            }
        } else {
            syncState.position?.let { seek(it, {}, PlaybackOrigin.RemoteSync) }
            syncState.playState?.let { playState(it, PlaybackOrigin.RemoteSync) }
        }
    }

    actual override fun addTrack(media: Track, index: Int) {
        _state.update { it.withTrackAdded(media, index) }
    }

    actual override fun updateQueue(tracks: List<Track>) {
        _state.update { state -> state.copy(queue = tracks, index = tracks.indexOfFirst { it.id == state.current?.id }.coerceAtLeast(0)) }
    }

    actual override fun moveTrack(from: Int, to: Int) {
        _state.update { state ->
            if (from !in state.queue.indices || to !in state.queue.indices || from == to) state
            else {
                val queue = state.queue.toMutableList().apply { add(to, removeAt(from)) }
                state.copy(queue = queue, index = queue.indexOfFirst { it.id == state.current?.id }.coerceAtLeast(0))
            }
        }
    }

    actual override fun removeTrack(index: Int) {
        _state.update { it.withTrackRemoved(index) }
    }

    /** Commands reported by the player must not be re-applied as new user input. */
    private fun PlaybackOrigin.controllerEventSource() = when (val source = toEventSource()) {
        EventSource.User -> EventSource.Player
        else -> source
    }
}

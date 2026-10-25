package com.eimsound.eimusic.playback

import com.eimsound.eimusic.db.DbLibraryRepository
import com.eimsound.eimusic.db.DbSessionRepository
import com.eimsound.eimusic.db.toDbTrack
import com.eimsound.eimusic.db.toTrack
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.music.change
import com.eimsound.eimusic.music.name
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.together.ChannelState
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.TogetherStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * 与壳无关的播放/队列/持久化逻辑。
 * UI 壳只负责输入、布局和平台事件桥接。
 */
class PlaybackStore(
    private val controller: IMediaPlayerController,
    private val storage: IStorage,
    private val library: DbLibraryRepository,
    private val session: DbSessionRepository,
    private val togetherStore: TogetherStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val state: StateFlow<PlayerState> = controller.state

    private val _recentTracks = MutableStateFlow(library.recentTracks().map { it.toTrack() })
    val recentTracks: StateFlow<List<Track>> = _recentTracks.asStateFlow()

    private val _pendingDelta = MutableStateFlow(0L)
    val displayPosition: StateFlow<Long> = combine(state, _pendingDelta) { s, delta ->
        val max = s.duration.coerceAtLeast(0L)
        (s.position + delta).coerceIn(0L, max)
    }.stateIn(scope, SharingStarted.Eagerly, 0L)

    private var debounceJob: Job? = null

    init {
        val savedVolume = storage.get(Settings::volume, 1.0f)
        val savedMuted = storage.get(Settings::isMuted, false)
        val savedPlayMode = storage.get(Settings::playMode, PlayMode.Loop.name)
        controller.volume(savedVolume, savedMuted)
        controller.playMode(PlayMode.of(savedPlayMode))

        val savedSession = session.load()
        if (savedSession.queue.isNotEmpty() || savedSession.currentTrackId != null) {
            val queue = savedSession.queue.map { it.toTrack() }
            val currentId = savedSession.currentTrackId?.let {
                runCatching { com.eimsound.eimusic.music.TrackId.parse(it) }.getOrNull()
            }
            controller.restore(queue, currentId)
        }

        scope.launch {
            state.map { it.current?.id }.distinctUntilChanged().collect {
                _pendingDelta.value = 0L
                debounceJob?.cancel()
                debounceJob = null
            }
        }

        if (_recentTracks.value.isEmpty()) {
            storage.get<Settings, List<Track>>(Settings::recentTracks, emptyList()).forEach {
                library.recordRecent(it.toDbTrack())
            }
            _recentTracks.value = library.recentTracks().map { it.toTrack() }
        }

        scope.launch {
            state.map { it.queue to it.current?.id }
                .distinctUntilChanged()
                .collect { (queue, currentId) ->
                    session.savePlayback(queue.map { it.toDbTrack() }, currentId?.toString())
                }
        }

        scope.launch {
            state.map { it.current }
                .distinctUntilChanged()
                .collect { track -> if (track != null) recordRecentTrack(track) }
        }

        scope.launch {
            state.map { Triple(it.volume, it.isMute, it.playMode.name) }
                .distinctUntilChanged()
                .collect { (volume, isMute, playMode) ->
                    storage.save(Settings::volume, volume)
                    storage.save(Settings::isMuted, isMute)
                    storage.save(Settings::playMode, playMode)
                }
        }
    }

    private fun recordRecentTrack(track: Track) {
        val updated = (listOf(track) + _recentTracks.value.filterNot { it.id == track.id }).take(12)
        if (updated == _recentTracks.value) return
        _recentTracks.value = updated
        library.recordRecent(track.toDbTrack())
    }

    private fun togetherJoined(): Boolean =
        togetherStore.state.value.channelState is ChannelState.Joined

    private fun rejectLocalTrack(track: Track): Boolean {
        if (togetherJoined() && track.source == TrackSource.LOCAL) {
            togetherStore.notifyLocalUnsupported()
            return true
        }
        return false
    }

    fun playNext(track: Track) = addToQueue(track, AddTrackStrategy.AddToNext)

    fun addToQueue(track: Track, strategy: AddTrackStrategy) {
        if (rejectLocalTrack(track)) return
        val index = when (strategy) {
            is AddTrackStrategy.AddToLast -> state.value.queue.size
            is AddTrackStrategy.AddToNext -> (state.value.index + 1).coerceIn(0, state.value.queue.size)
            is AddTrackStrategy.InsertToQueue -> strategy.index.coerceIn(0, state.value.queue.size)
        }
        controller.addTrack(track, index)
        EventBus.PlayingListEventBus.send(
            PlayingListEvent.TrackAdded(track, strategy, source = EventSource.User),
        )
    }

    fun playTrackFromList(tracks: List<Track>, track: Track) {
        if (tracks.isEmpty()) return
        val playable = if (togetherJoined()) tracks.filter { it.source != TrackSource.LOCAL } else tracks
        if (playable.isEmpty() || (togetherJoined() && track.source == TrackSource.LOCAL)) {
            togetherStore.notifyLocalUnsupported()
            return
        }
        controller.queue(playable)
        controller.play(track)
        announceQueueChange()
    }

    fun playTrack(track: Track) {
        if (rejectLocalTrack(track)) return
        val added = state.value.queue.none { it.id == track.id }
        if (added) controller.addTrack(track, state.value.queue.size)
        controller.play(track)
        if (added) {
            EventBus.PlayingListEventBus.send(
                PlayingListEvent.TrackAdded(track, AddTrackStrategy.AddToLast, source = EventSource.User),
            )
        }
    }

    fun playAll(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val playable = if (togetherJoined()) tracks.filter { it.source != TrackSource.LOCAL } else tracks
        if (playable.isEmpty()) {
            togetherStore.notifyLocalUnsupported()
            return
        }
        controller.queue(playable) { controller.play(playable.first()) }
        announceQueueChange()
    }

    fun removeTrack(index: Int) {
        val track = state.value.queue.getOrNull(index)
        controller.removeTrack(index)
        if (track != null) {
            EventBus.PlayingListEventBus.send(
                PlayingListEvent.TrackRemoved(track, source = EventSource.User),
            )
        }
    }

    fun moveTrack(from: Int, to: Int) {
        controller.moveTrack(from, to)
        EventBus.PlayingListEventBus.send(
            PlayingListEvent.TrackMoved(from, to, source = EventSource.User),
        )
    }

    fun clearQueue() {
        controller.playState(PlayState.STOPPED)
        for (i in (state.value.queue.size - 1) downTo 0) controller.removeTrack(i)
        announceQueueChange()
    }

    /** compose 的事件桥：应用来自 EventBus 的队列事件，不触发本地入队拦截。 */
    fun applyTrackListChanged(tracks: List<Track>, track: Track?, source: EventSource) {
        controller.queue(tracks) {
            if (source != EventSource.Player) track?.let { controller.play(it) }
        }
    }

    fun applyTrackAdded(track: Track, strategy: AddTrackStrategy) {
        val index = when (strategy) {
            is AddTrackStrategy.AddToLast -> state.value.queue.size
            is AddTrackStrategy.AddToNext -> state.value.index + 1
            is AddTrackStrategy.InsertToQueue -> strategy.index
        }
        controller.addTrack(track, index)
    }

    fun applyTrackRemoved(track: Track) {
        val index = state.value.queue.indexOfFirst { it.id == track.id }
        if (index >= 0) controller.removeTrack(index)
    }

    fun togglePlayPause() {
        controller.playState(
            if (state.value.playState == PlayState.PLAYING) PlayState.PAUSED else PlayState.PLAYING,
        )
    }

    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seek(millis: Long) = controller.seek(millis)

    fun seekRelative(delta: Long) {
        _pendingDelta.value += delta
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(SEEK_DEBOUNCE_MS.milliseconds)
            flushPendingSeek()
        }
    }

    private fun flushPendingSeek() {
        val offset = _pendingDelta.value
        if (offset == 0L) return
        _pendingDelta.value = 0L
        val max = state.value.duration.coerceAtLeast(0L)
        val target = (state.value.position + offset).coerceIn(0L, max)
        controller.seek(target)
    }

    fun togglePlayMode() {
        controller.playMode(state.value.playMode.change { state.value.queue.size })
    }

    fun volumeUp() {
        val steps = ((state.value.volume * 10f) + 0.001f).toInt().coerceIn(0, 10)
        controller.volume((steps + 1).coerceAtMost(10) / 10f, state.value.isMute)
    }

    fun adjustVolume(delta: Int) {
        val steps = ((state.value.volume * 10f) + 0.001f).toInt().coerceIn(0, 10)
        controller.volume((steps + delta).coerceIn(0, 10) / 10f, false)
    }

    fun volumeDown() {
        val steps = ((state.value.volume * 10f) + 0.001f).toInt().coerceIn(0, 10)
        controller.volume((steps - 1).coerceAtLeast(0) / 10f, state.value.isMute)
    }

    fun toggleMute() = controller.volume(state.value.volume, !state.value.isMute)

    fun setPlayMode(mode: PlayMode) = controller.playMode(mode)
    fun setVolume(value: Float) = controller.volume(value)
    fun setMute(value: Boolean) = controller.volume(state.value.volume, value)

    fun release() {
        scope.cancel()
    }

    private fun announceQueueChange() {
        EventBus.PlayingListEventBus.send(
            PlayingListEvent.TrackListChanged(
                tracks = state.value.queue,
                track = state.value.current,
                source = EventSource.User,
            ),
        )
    }

    companion object {
        private const val SEEK_DEBOUNCE_MS = 300L
    }
}

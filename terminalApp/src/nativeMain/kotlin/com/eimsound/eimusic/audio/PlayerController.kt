package com.eimsound.eimusic.audio

import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.eimsound.eimusic.util.TuiLogger
import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.change
import com.eimsound.eimusic.music.nextTrackCandidate
import com.eimsound.eimusic.music.previousTrackCandidate
import com.eimsound.eimusic.music.recalculatedIndex
import com.eimsound.eimusic.music.withTrackAdded
import com.eimsound.eimusic.music.withTrackRemoved
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import io.ktor.client.HttpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class PlayerController(
    private val engine: NativeAudioEngine,
    private val httpClient: HttpClient? = null,
) : IMediaPlayerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
        TuiLogger.log("CRASH: ${e.message}")
    })

    private val _state = MutableStateFlow(
        PlayerState(playState = PlayState.STOPPED, volume = 1f)
    )
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 8)
    override val event: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    private var positionJob: Job? = null
    private var playJob: Job? = null
    private var streamJob: Job? = null
    private var networkStream: NetworkStream? = null

    /** 会话恢复后尚未真正打开音频；用户首次播放时再加载。 */
    private var needsOpen = false

    private var syncPendingPosition: Long? = null
    private var syncPendingPlayState: PlayState? = null

    init {
        scope.launch {
            EventBus.PlaybackEventBus.receive { event ->
                if (event.source != EventSource.Remote) return@receive
                when (event) {
                    is PlaybackEvent.PlayStateChanged -> {
                        if (event.isPlaying) {
                            resume()
                        } else {
                            pause()
                        }
                        event.position?.let { seek(it.milliseconds) }
                    }
                    is PlaybackEvent.TrackChanged -> play(event.track)
                    is PlaybackEvent.Seek -> seek(event.position.milliseconds)
                    is PlaybackEvent.PlayModeChanged -> _state.update { it.copy(playMode = event.playMode) }
                    is PlaybackEvent.Next -> {
                        when (_state.value.playMode) {
                            is PlayMode.Shuffle, is PlayMode.Loop -> {
                                val next = nextTrack()
                                if (next != null) play(next, event.isAudioCompleted)
                            }
                            is PlayMode.RepeatOne -> seek(0) {}
                        }
                    }
                    is PlaybackEvent.Previous -> {
                        when (_state.value.playMode) {
                            is PlayMode.Shuffle, is PlayMode.Loop -> {
                                val prev = previousTrack()
                                if (prev != null) play(prev, false)
                            }
                            is PlayMode.RepeatOne -> seek(0) {}
                        }
                    }
                }
            }
        }
        scope.launch {
            EventBus.PlayingListEventBus.receive { event ->
                if (event.source != EventSource.Remote) return@receive
                when (event) {
                    is PlayingListEvent.TrackListChanged -> queue(event.tracks, event.track)
                    is PlayingListEvent.TrackAdded -> {
                        val insertIndex = when (val s = event.strategy) {
                            is AddTrackStrategy.AddToNext -> state.value.index + 1
                            is AddTrackStrategy.InsertToQueue -> s.index
                            is AddTrackStrategy.AddToLast -> state.value.queue.size
                        }
                        addTrack(event.track, insertIndex)
                    }
                    is PlayingListEvent.TrackRemoved -> {
                        val idx = state.value.queue.indexOfFirst { it.id == event.track.id }
                        if (idx >= 0) removeTrack(idx)
                    }
                }
            }
        }
    }

    // ── Queue management ──

    override fun queue(medias: List<Track>, prepareOver: () -> Unit) {
        queue(medias, startTrack = null)
        prepareOver()
    }

    fun queue(tracks: List<Track>, startTrack: Track? = null) {
        _state.update {
            it.copy(
                queue = tracks,
                index = tracks.indexOfFirst { t -> t.id == startTrack?.id },
                current = startTrack ?: it.current,
            )
        }

        if (startTrack != null) doOpenAndPlay(startTrack)
    }

    override fun play(media: Track) {
        play(media, false)
    }

    private fun play(media: Track, isAudioCompleted: Boolean = false) {
        needsOpen = false
        val list = currentList()
        _state.update {
            it.copy(
                index = list.indexOfFirst { track -> track.id == media.id },
                current = media,
                isLoading = true
            )
        }

        doOpenAndPlay(media)
    }

    // ── Playback control ──

    fun resume() {
        if (needsOpen) {
            val track = _state.value.current
            if (track != null) {
                needsOpen = false
                play(track)
                return
            }
        }
        engine.play()
        _state.update { it.copy(playState = PlayState.PLAYING) }
        startPositionPolling()
    }

    fun pause() {
        engine.pause()
        _state.update { it.copy(playState = PlayState.PAUSED) }
        stopPositionPolling()
    }

    fun togglePlayPause() {
        if (engine.isPlaying()) pause() else resume()
    }

    override fun playState(state: PlayState) {
        when (state) {
            PlayState.PLAYING -> resume()
            PlayState.PAUSED -> pause()
            PlayState.STOPPED -> {
                engine.stop()
                _state.update { it.copy(playState = PlayState.STOPPED, position = 0) }
                stopPositionPolling()
            }
        }
        EventBus.PlaybackEventBus.send(
            PlaybackEvent.PlayStateChanged(state == PlayState.PLAYING, position = Duration(_state.value.position), source = EventSource.Player)
        )
    }

    // ── Track navigation ──

    override fun next(isAudioCompleted: Boolean) {
        when (_state.value.playMode) {
            is PlayMode.Shuffle, is PlayMode.Loop -> {
                val next = nextTrack()
                if (next != null) {
                    EventBus.PlaybackEventBus.send(
                        PlaybackEvent.Next(isAudioCompleted, next, source = EventSource.Player)
                    )
                    play(next, isAudioCompleted)
                }
            }
            is PlayMode.RepeatOne -> seek(0) {}
        }
    }

    override fun nextTrack(): Track? = state.value.nextTrackCandidate()

    override fun previous() {
        when (_state.value.playMode) {
            is PlayMode.Shuffle, is PlayMode.Loop -> {
                val prev = previousTrack()
                if (prev != null) {
                    EventBus.PlaybackEventBus.send(
                        PlaybackEvent.Previous(prev, source = EventSource.Player)
                    )
                    play(prev, false)
                }
            }
            is PlayMode.RepeatOne -> seek(0) {}
        }
    }

    override fun previousTrack(): Track? = state.value.previousTrackCandidate()

    // ── Seek & volume ──

    override fun seek(millis: Long, seekOver: () -> Unit) {
        engine.seek(millis)
        _state.update { it.copy(position = millis) }
        EventBus.PlaybackEventBus.send(
            PlaybackEvent.Seek(Duration(millis), source = EventSource.Player)
        )
        seekOver()
    }

    fun seek(millis: Long) {
        engine.seek(millis)
        _state.update { it.copy(position = millis) }
    }

    fun setVolume(value: Float) {
        engine.setVolume(value)
        _state.update { it.copy(volume = value.coerceIn(0f, 1f)) }
    }

    override fun volume(volume: Float, isMuted: Boolean) {
        val actual = if (isMuted) 0f else volume
        engine.setVolume(actual)
        _state.update { it.copy(volume = volume.coerceIn(0f, 1f), isMute = isMuted) }
    }

    // ── Play mode ──

    fun togglePlayMode() {
        _state.update {
            val newMode = it.playMode.change { it.queue.size }
            it.copy(playMode = newMode)
        }
    }

    override fun playMode(mode: PlayMode) {
        _state.update { it.copy(playMode = mode) }
        EventBus.PlaybackEventBus.send(
            PlaybackEvent.PlayModeChanged(mode, source = EventSource.Player)
        )
    }

    // ── Queue editing ──

    override fun addTrack(media: Track, index: Int) {
        _state.update { it.withTrackAdded(media, index) }
        EventBus.PlayingListEventBus.send(
            PlayingListEvent.TrackAdded(media, AddTrackStrategy.AddToLast, source = EventSource.Player)
        )
    }

    override fun removeTrack(index: Int) {
        val removed = _state.value.queue.getOrNull(index)
        _state.update { it.withTrackRemoved(index) }
        if (removed != null) {
            EventBus.PlayingListEventBus.send(
                PlayingListEvent.TrackRemoved(removed, source = EventSource.Player)
            )
        }
    }

    override fun moveTrack(from: Int, to: Int) {
        _state.update {
            val mutable = it.queue.toMutableList()
            if (from in mutable.indices && to in mutable.indices) {
                val item = mutable.removeAt(from)
                mutable.add(to, item)
            }
            it.copy(queue = mutable, index = it.recalculatedIndex(mutable))
        }
    }

    fun clearQueue() {
        engine.stop()
        needsOpen = false
        _state.update { it.copy(queue = emptyList(), current = null, playState = PlayState.STOPPED, position = 0) }
    }

    override fun restore(queue: List<Track>, currentId: com.eimsound.eimusic.music.TrackId?) {
        val index = queue.indexOfFirst { it.id == currentId }
        val current = queue.getOrNull(index)
        _state.update {
            it.copy(
                queue = queue,
                index = index,
                current = current,
                playState = PlayState.PAUSED,
                position = 0,
                isLoading = false,
            )
        }
        // 只恢复 UI/队列，不打开音频流；首次播放时再加载。
        needsOpen = current != null
    }

    // ── Lifecycle ──

    override fun release() {
        streamJob?.cancel()
        streamJob = null
        networkStream?.close()
        networkStream = null
        stopPositionPolling()
        engine.release()
        scope.cancel()
    }

    override fun sync(syncState: SyncState) {
        _state.update {
            it.copy(
                queue = syncState.trackList ?: it.queue,
                current = syncState.track ?: it.current,
                playMode = syncState.playMode ?: it.playMode,
                playState = syncState.playState ?: it.playState,
                position = syncState.position ?: it.position,
            )
        }
        val newTrack = syncState.track
        if (newTrack != null) {
            syncPendingPosition = syncState.position
            syncPendingPlayState = syncState.playState
            play(newTrack)
        } else {
            syncState.playState?.let { playState(it) }
            syncState.position?.let { seek(it) }
        }
    }

    // ── Private helpers ──

    private fun currentList(): List<Track> = _state.value.currentList

    private fun doOpenAndPlay(track: Track) {
        playJob?.cancel()
        streamJob?.cancel()
        streamJob = null
        engine.closeCurrent()
        networkStream?.close()
        networkStream = null
        stopPositionPolling()
        playJob = scope.launch {
            try {
                TuiLogger.log("Track Source: ${track.name} (${track.source})")
                val resolver = runCatching { TrackUriResolverFactory.get(track.source) }.getOrNull()
                if (resolver == null) {
                    TuiLogger.log("No resolver for ${track.source}"); return@launch
                }
                val uri = resolver.resolve(track) ?: track.uri
                TuiLogger.log("Resolved URI: ${uri?.take(80)}")
                val playUri = uri ?: run { TuiLogger.log("URI is null"); return@launch }

                if (playUri.startsWith("http://") || playUri.startsWith("https://")) {
                    val cachePath = cacheFilePath(track.id.toString())
                    if (fileExists(cachePath)) {
                        TuiLogger.log("Cache hit: $cachePath")
                        val dur = engine.openFile(cachePath)
                        applyPlaybackState(dur)
                    } else {
                        playNetworkStream(playUri, cachePath)
                    }
                } else {
                    val dur = engine.openFile(playUri)
                    applyPlaybackState(dur)
                }
            } catch (e: Exception) {
                TuiLogger.log("Playback failed: ${e.message}")
                _state.update { it.copy(isLoading = false) }
                next()
            }
        }
    }

    private suspend fun playNetworkStream(uri: String, cachePath: String) {
        ensureCacheDir()
        val stream = NetworkStream(uri, httpClient!!, cachePath)
        networkStream = stream
        TuiLogger.log("Starting network stream...")
        streamJob = scope.launch { stream.start() }
        stream.waitForPrebuffer()
        check(stream.getBuffer().available() > 0) { "stream produced no audio data" }
        val dur = engine.openStream(stream.getBuffer())
        applyPlaybackState(dur)
    }

    private fun applyPlaybackState(dur: Long) {
        _state.update { it.copy(duration = dur.coerceAtLeast(0L), isLoading = false, position = 0) }
        val s = _state.value
        engine.setVolume(if (s.isMute) 0f else s.volume)

        val pos = syncPendingPosition
        val playSt = syncPendingPlayState
        syncPendingPosition = null
        syncPendingPlayState = null

        when (playSt) {
            PlayState.PAUSED -> {
                _state.update { it.copy(playState = PlayState.PAUSED, position = pos ?: 0) }
                if (pos != null && pos > 0) engine.seek(pos)
                TuiLogger.log("Sync: loaded track, staying paused")
            }
            PlayState.STOPPED -> {
                engine.stop()
                _state.update { it.copy(playState = PlayState.STOPPED, position = 0) }
                TuiLogger.log("Sync: loaded track, staying stopped")
            }
            else -> {
                engine.play()
                _state.update { it.copy(playState = PlayState.PLAYING) }
                if (pos != null && pos > 0) engine.seek(pos)
                TuiLogger.log("Sync: playback started")
                startPositionPolling()
            }
        }
    }

    private fun cacheFilePath(trackId: String): String {
        val safeName = trackId.replace(':', '_').replace('/', '_')
        return "/tmp/eimusic_play/$safeName"
    }

    private fun fileExists(path: String): Boolean =
        SystemFileSystem.exists(Path(path))

    private fun ensureCacheDir() {
        SystemFileSystem.createDirectories(Path("/tmp/eimusic_play"))
    }

    private fun startPositionPolling() {
        stopPositionPolling()
        positionJob = scope.launch {
            while (isActive) {
                val pos = engine.position()
                _state.update { it.copy(position = pos) }

                if (!engine.isPlaying() && _state.value.playState == PlayState.PLAYING) {
                    val currentPos = engine.position()
                    val dur = engine.duration()
                    if (dur > 0 && currentPos >= dur - 500) {
                        next(true)
                    } else {
                        _state.update { it.copy(playState = PlayState.PAUSED) }
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopPositionPolling() {
        positionJob?.cancel()
        positionJob = null
    }

}

package com.eimsound.eimusic.viewmodel

import com.eimsound.eimusic.db.DbLibraryRepository
import com.eimsound.eimusic.db.DbSessionRepository
import com.eimsound.eimusic.db.DbTrack
import com.eimsound.eimusic.db.toDbTrack
import com.eimsound.eimusic.db.toTrack
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.change
import com.eimsound.eimusic.music.name
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.together.ChannelState
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.music.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class PlayerViewModel(
    private val controller: IMediaPlayerController,
    private val storage: IStorage,
    private val library: DbLibraryRepository,
    private val session: DbSessionRepository,
    private val channelViewModel: ChannelViewModel,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val state: StateFlow<PlayerState> = controller.state

    private val _recentTracks = MutableStateFlow(library.recentTracks().map { it.toTrack() })

    /** 最近播放（新→旧），用于 Main 空状态。 */
    val recentTracks: StateFlow<List<Track>> = _recentTracks.asStateFlow()

    /** 积累式 seek 偏移量 */
    private val _pendingDelta = MutableStateFlow(0L)

    /** UI 显示的进度位置（引擎位置 + 积累偏移），变化时立即触发重组 */
    val displayPosition: StateFlow<Long> = combine(
        state,
        _pendingDelta,
    ) { s, delta ->
        (s.position + delta).coerceIn(0, s.duration)
    }.stateIn(scope, SharingStarted.Eagerly, 0L)

    private var debounceJob: Job? = null

    init {
        // 启动时从配置文件恢复音量、静音和循环模式
        val savedVolume = storage.get(Settings::volume, 1.0f)
        val savedMuted = storage.get(Settings::isMuted, false)
        val savedPlayMode = storage.get(Settings::playMode, PlayMode.Loop.name)
        controller.volume(savedVolume, savedMuted)
        controller.playMode(PlayMode.of(savedPlayMode))

        // 恢复上次队列与当前曲目（暂停，不恢复位置）。
        val savedSession = session.load()
        if (savedSession.queue.isNotEmpty() || savedSession.currentTrackId != null) {
            val queue = savedSession.queue.map { it.toTrack() }
            val currentId = savedSession.currentTrackId?.let {
                runCatching { com.eimsound.eimusic.music.TrackId.parse(it) }.getOrNull()
            }
            controller.restore(queue, currentId)
        }

        // 切歌时清掉尚未 flush 的 seek 偏移，避免新曲目带上旧进度。
        scope.launch {
            state.map { it.current?.id }
                .distinctUntilChanged()
                .collect {
                    _pendingDelta.value = 0L
                    debounceJob?.cancel()
                    debounceJob = null
                }
        }

        // 首次启动把旧 Settings 中的最近播放迁到 DB。
        if (_recentTracks.value.isEmpty()) {
            storage.get<Settings, List<Track>>(Settings::recentTracks, emptyList()).forEach {
                library.recordRecent(it.toDbTrack())
            }
            _recentTracks.value = library.recentTracks().map { it.toTrack() }
        }

        // 队列/当前曲目变化时持久化到 session。
        scope.launch {
            controller.state
                .map { it.queue to it.current?.id }
                .distinctUntilChanged()
                .collect { (queue, currentId) ->
                    session.savePlayback(queue.map { it.toDbTrack() }, currentId?.toString())
                }
        }

        // 记录最近播放
        scope.launch {
            controller.state
                .map { it.current }
                .distinctUntilChanged()
                .collect { track ->
                    if (track != null) recordRecentTrack(track)
                }
        }

        // 后台收集状态变化，自动持久化到配置文件
        scope.launch {
            controller.state
                .map { Triple(it.volume, it.isMute, it.playMode.name) }
                .distinctUntilChanged()
                .collect { (volume, isMute, playMode) ->
                    storage.save(Settings::volume, volume)
                    storage.save(Settings::isMuted, isMute)
                    storage.save(Settings::playMode, playMode)
                }
        }
    }

    private fun recordRecentTrack(track: Track) {
        val updated = (listOf(track) + _recentTracks.value.filterNot { it.id == track.id })
            .take(12)
        if (updated == _recentTracks.value) return
        _recentTracks.value = updated
        library.recordRecent(track.toDbTrack())
    }

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
        val target = (state.value.position + offset).coerceIn(0, state.value.duration)
        controller.seek(target)
    }

    fun togglePlayPause() {
        val s = state.value.playState
        controller.playState(
            if (s == PlayState.PLAYING) PlayState.PAUSED else PlayState.PLAYING
        )
    }

    fun next() = controller.next()

    fun previous() = controller.previous()

    fun seek(millis: Long) = controller.seek(millis)

    fun togglePlayMode() {
        val newMode = state.value.playMode.change { state.value.queue.size }
        controller.playMode(newMode)
    }

    fun volumeUp() {
        val steps = ((state.value.volume * 10f) + 0.001f).toInt().coerceIn(0, 10)
        controller.volume((steps + 1).coerceAtMost(10) / 10f, state.value.isMute)
    }

    /** 滚轮调节：改变音量并解除静音。 */
    fun adjustVolume(delta: Int) {
        val steps = ((state.value.volume * 10f) + 0.001f).toInt().coerceIn(0, 10)
        val next = (steps + delta).coerceIn(0, 10)
        controller.volume(next / 10f, false)
    }

    fun volumeDown() {
        val steps = ((state.value.volume * 10f) + 0.001f).toInt().coerceIn(0, 10)
        controller.volume((steps - 1).coerceAtLeast(0) / 10f, state.value.isMute)
    }

    fun toggleMute() {
        // 切换静音，而不是“只要音量 > 0 就静音”；否则点击音量图标后无法再打开。
        controller.volume(state.value.volume, !state.value.isMute)
    }

    private fun togetherJoined(): Boolean =
        channelViewModel.uiState.value.channelState is ChannelState.Joined

    private fun rejectLocalTrack(track: Track): Boolean {
        if (togetherJoined() && track.source == TrackSource.LOCAL) {
            channelViewModel.notifyLocalUnsupported()
            return true
        }
        return false
    }

    fun playNext(track: Track) {
        if (rejectLocalTrack(track)) return
        val index = (state.value.index + 1).coerceIn(0, state.value.queue.size)
        controller.addTrack(track, index)
        EventBus.PlayingListEventBus.send(
            PlayingListEvent.TrackAdded(track, AddTrackStrategy.AddToNext, source = EventSource.User),
        )
    }

    /** 用当前列表替换队列，并播放选中曲目。 */
    fun playTrackFromList(tracks: List<Track>, track: Track) {
        if (tracks.isEmpty()) return
        val playable = if (togetherJoined()) {
            tracks.filter { it.source != TrackSource.LOCAL }
        } else {
            tracks
        }
        if (playable.isEmpty() || (togetherJoined() && track.source == TrackSource.LOCAL)) {
            channelViewModel.notifyLocalUnsupported()
            return
        }
        controller.queue(playable)
        controller.play(track)
        announceQueueChange()
    }

    fun playTrack(track: Track) {
        if (rejectLocalTrack(track)) return
        // 已在队列里的曲目直接播放，不移动位置；新曲目才追加到末尾。
        val added = state.value.queue.none { it.id == track.id }
        if (added) {
            controller.addTrack(track, state.value.queue.size)
        }
        controller.play(track)
        if (added) {
            EventBus.PlayingListEventBus.send(
                PlayingListEvent.TrackAdded(track, AddTrackStrategy.AddToLast, source = EventSource.User),
            )
        }
    }

    fun playAll(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val playable = if (togetherJoined()) {
            tracks.filter { it.source != TrackSource.LOCAL }
        } else {
            tracks
        }
        if (playable.isEmpty()) {
            channelViewModel.notifyLocalUnsupported()
            return
        }
        controller.queue(playable) {
            controller.play(playable.first())
        }
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
        val size = state.value.queue.size
        for (i in (size - 1) downTo 0) {
            controller.removeTrack(i)
        }
        announceQueueChange()
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

    fun release() {
        scope.cancel()
    }

    companion object {
        private const val SEEK_DEBOUNCE_MS = 300L
    }
}

package com.eimsound.eimusic.together

import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.network.TogetherWebSocketClient
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.playback.PlaybackOrigin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 播放同步管理器
 * 负责协调本地播放与远程同步状态
 */
class TogetherSyncManager(
    private val controller: IMediaPlayerController,
    private val webSocketClient: TogetherWebSocketClient
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastSyncState: SyncState? = null
    private var queueRevision = 0L
    private val pendingCommands = MutableStateFlow<Set<String>>(emptySet())

    private val _channelState = MutableStateFlow<ChannelState>(ChannelState.Disconnected)
    val channelState: StateFlow<ChannelState> = _channelState.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun updateChannelState(state: ChannelState) {
        _channelState.value = state
    }

    fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    /**
     * 处理远程同步状态
     */
    fun handleRemoteSyncState(remoteStateEvent: ChannelEvent.PlayStateChanged) {
        // 已退出频道后不再接受任何播放控制（包括 leave 前已经到达的事件）。
        if (_channelState.value !is ChannelState.Joined) return
        val remoteState = remoteStateEvent.state
        lastSyncState = remoteState
        queueRevision = remoteState.queueRevision
        val commandId = remoteState.commandId
        if (commandId != null && commandId in pendingCommands.value) {
            pendingCommands.update { it - commandId }
            // 自己的命令：只确认，不重复应用。
            return
        }
        // 兼容没有 commandId 的旧状态。
        if (commandId == null && remoteState.controlledBy?.id == webSocketClient.currentUserId()) return

        val event = remoteStateEvent.event
        try {
            when (event) {
                is PlayControlEvent.AddToQueue -> addToQueue(event.track, event.strategy)
                is PlayControlEvent.RemoveFromQueue -> removeFromQueue(event.track)
                is PlayControlEvent.ChangeTrack -> synchronizeTrack(event.track, event.isAudioCompleted)
                is PlayControlEvent.ChangeTrackList -> synchronizeTrackList(event.trackList, event.play)
                is PlayControlEvent.QueueChange -> synchronizeTrackList(
                    remoteState.trackList ?: emptyList(),
                    remoteState.track,
                )
                // 使用 TrackChanged
                is PlayControlEvent.Next -> {
                    controller.next(origin = PlaybackOrigin.RemoteSync)
                }

                is PlayControlEvent.Previous -> {
                    controller.previous(PlaybackOrigin.RemoteSync)
                }
                is PlayControlEvent.Pause -> synchronizePlayState(
                    remoteState.playState ?: PlayState.PAUSED,
                    event.position,
                    remoteState.timestamp
                )

                is PlayControlEvent.Play -> synchronizePlayState(
                    remoteState.playState ?: PlayState.PLAYING,
                    event.position,
                    remoteState.timestamp
                )

                is PlayControlEvent.Stop -> synchronizePlayState(remoteState.playState ?: PlayState.STOPPED)
                is PlayControlEvent.PlayModeChanged -> synchronizePlayMode(event.mode)
                is PlayControlEvent.Seek -> synchronizePosition(event.position)
            }
        } finally {
        }
    }

    private fun removeFromQueue(track: Track) {
        EventBus.PlayingListEventBus.send(PlayingListEvent.TrackRemoved(track, source = EventSource.Remote))
    }

    private fun addToQueue(track: Track, strategy: AddTrackStrategy) {
        EventBus.PlayingListEventBus.send(PlayingListEvent.TrackAdded(track, strategy, source = EventSource.Remote))
    }

    private fun synchronizeTrackList(trackList: List<Track>, play: Track? = null) {
        if (trackList.isEmpty()) controller.playState(PlayState.STOPPED, PlaybackOrigin.RemoteSync)
        EventBus.PlayingListEventBus.send(
            PlayingListEvent.TrackListChanged(
                trackList,
                play,
                source = EventSource.Remote
            )
        )
    }


    private fun safePosition(position: Long): Long {
        val localPosition = if (controller.state.value.duration <= 0L) 0L else controller.state.value.duration
        val safePosition = position.coerceIn(0L, localPosition)
        return safePosition
    }

    private fun synchronizePosition(position: Long, seekOver: () -> Unit = {}) {
        if (abs(position - controller.state.value.position) > 100) {
            EventBus.PlaybackEventBus.send(
                PlaybackEvent.Seek(
                    Duration(safePosition(position)),
                    seekOver,
                    source = EventSource.Remote
                )
            )
        }
    }


    private fun synchronizePlayMode(remotePlayMode: PlayMode) {
        EventBus.PlaybackEventBus.send(PlaybackEvent.PlayModeChanged(remotePlayMode, source = EventSource.Remote))
    }

    /**
     * 同步播放状态
     */
    private fun synchronizePlayState(
        remotePlayState: PlayState,
        position: Long? = null,
        serverTimestamp: Instant? = null
    ) {
        val isPlaying = when (remotePlayState) {
            PlayState.PLAYING -> true
            PlayState.PAUSED -> false
            PlayState.STOPPED -> false
        }
        val now = Clock.System.now()
        val finalPosition = if (serverTimestamp != null && remotePlayState == PlayState.PLAYING) {
            val timeDiff = now - serverTimestamp  // 计算网络延迟
            val timeDiffMs = if (timeDiff.inWholeMilliseconds < 0) 0 else timeDiff.inWholeMilliseconds
            val adjustedPosition = (position ?: 0L) + timeDiffMs  // 补偿延迟
            safePosition(adjustedPosition)
        } else {
            safePosition(position ?: 0)
        }

        EventBus.PlaybackEventBus.send(
            PlaybackEvent.PlayStateChanged(
                isPlaying,
                Duration(finalPosition),
                source = EventSource.Remote
            )
        )
    }

    /**
     * 同步曲目
     */
    private fun synchronizeTrack(
        remoteTrack: Track,
        isAudioCompleted: Boolean = false,
        autoPlay: Boolean = true,
        prepareOver: () -> Unit = {}
    ) {
        EventBus.PlaybackEventBus.send(
            PlaybackEvent.TrackChanged(
                remoteTrack,
                isAudioCompleted,
                autoPlay,
                prepareOver,
                source = EventSource.Remote
            )
        )
    }


    /**
     * 发送播放控制命令
     */
    suspend fun sendPlayCommand(event: PlayControlEvent) {
        val commandId = "${webSocketClient.currentUserId()}-${Clock.System.now().toEpochMilliseconds()}-${kotlin.random.Random.nextLong().toULong().toString(16)}"
        pendingCommands.update { it + commandId }
        val result = webSocketClient.sendPlayControl(event, commandId)
        if (result.isFailure) pendingCommands.update { it - commandId }
    }

    /**
     * 获取最后同步的状态
     */
    fun getLastSyncState(): SyncState? = lastSyncState

    /**
     * 获取同步质量指标
     */
    fun getSyncQuality(): SyncQuality {
        val localPosition = controller.state.value.position
        val remotePosition = lastSyncState?.position ?: 0L
        val positionDiff = abs(localPosition - remotePosition)

        return when {
            positionDiff < 100 -> SyncQuality.EXCELLENT
            positionDiff < 500 -> SyncQuality.GOOD
            positionDiff < 1000 -> SyncQuality.FAIR
            else -> SyncQuality.POOR
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        updateChannelState(ChannelState.Disconnected)
        updateConnectionState(ConnectionState.DISCONNECTED)
        coroutineScope.cancel()
    }

    fun initializeState(channel: Channel) {
        queueRevision = channel.queueRevision
        val serverTimestamp = channel.anchorTimestamp
        val clientNow = Clock.System.now()

        val realProgress = if (channel.playState == PlayState.PLAYING) {
            val durationSinceAnchor = clientNow - serverTimestamp
            val calculatedPosition = channel.currentPosition + durationSinceAnchor.inWholeMilliseconds

            val maxDuration = controller.state.value.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
            calculatedPosition.coerceIn(0L, maxDuration)
        } else {
            channel.currentPosition
        }
        val syncState = SyncState(
            trackList = channel.trackList,
            track = channel.currentTrack,
            playMode = channel.playMode,
            playState = channel.playState,
            position = realProgress
        )
        controller.sync(syncState)
    }

    fun collectCurrentState(): SyncState {
        val state = controller.state.value
        // Together 只同步可跨端解析的在线曲目；本地路径对其他人无意义。
        val shareableQueue = state.queue.filter { it.source != com.eimsound.eimusic.music.TrackSource.LOCAL }
        val shareableCurrent = state.current?.takeIf { it.source != com.eimsound.eimusic.music.TrackSource.LOCAL }
        return SyncState(
            track = shareableCurrent,
            trackList = shareableQueue,
            playMode = state.playMode,
            playState = if (shareableCurrent == null) PlayState.STOPPED else state.playState,
            position = if (shareableCurrent == null) 0L else state.position,
            queueRevision = queueRevision,
        )
    }

    fun currentQueueRevision(): Long = queueRevision

    /** 混合本地曲目时索引操作不可靠，退回整队列快照。 */
    fun hasLocalTracks(): Boolean =
        controller.state.value.queue.any { it.source == com.eimsound.eimusic.music.TrackSource.LOCAL }

    fun isCurrentTrackLocal(): Boolean =
        controller.state.value.current?.source == com.eimsound.eimusic.music.TrackSource.LOCAL

    fun shouldIgnoreEventSource(source: EventSource): Boolean =
        channelState.value is ChannelState.Joined && source is EventSource.User

    val position: Long get() = controller.state.value.position
}

/**
 * 同步质量等级
 */
enum class SyncQuality {
    EXCELLENT, // 优秀 (< 100ms)
    GOOD,      // 良好 (< 500ms)
    FAIR,      // 一般 (< 1000ms)
    POOR       // 较差 (> 1000ms)
}
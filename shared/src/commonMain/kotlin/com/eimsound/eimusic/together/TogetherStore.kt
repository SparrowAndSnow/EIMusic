package com.eimsound.eimusic.together

import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.auth.AuthStore
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.network.TogetherWebSocketClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Together 的跨壳状态；输入框等纯 UI 状态留给各自壳。 */
data class TogetherState(
    val channelState: ChannelState = ChannelState.Disconnected,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val messages: List<ChannelMessage> = emptyList(),
    val members: List<User> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * 与壳无关的一起听逻辑：连接、频道、聊天、播放/队列事件转发。
 * terminalApp 和 composeApp 都只保留薄适配层。
 */
class TogetherStore(
    private val webSocketClient: TogetherWebSocketClient,
    private val syncManager: TogetherSyncManager,
    private val authStore: AuthStore,
    private val log: (String) -> Unit = {},
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow(TogetherState())
    val state: StateFlow<TogetherState> = _state.asStateFlow()

    /** 服务器聊天消息，供壳做系统通知；不参与状态。 */
    private val _incomingMessages = MutableSharedFlow<ChannelMessage>(extraBufferCapacity = 32)
    val incomingMessages: SharedFlow<ChannelMessage> = _incomingMessages.asSharedFlow()

    private var currentChannelId: String? = null
    private var localNoticeShown = false

    init {
        startObserving()
    }

    fun connect() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { webSocketClient.connect() }
                .onFailure { log("TOGETHER connect failed: ${it.message}") }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun disconnect() {
        scope.launch { webSocketClient.disconnect() }
    }

    fun joinChannel(channelId: String) {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val syncState = syncManager.collectCurrentState()
            webSocketClient.joinChannel(channelId, syncState)
                .onSuccess { channel ->
                    currentChannelId = channel.id
                    localNoticeShown = false
                    syncManager.updateChannelState(ChannelState.Joined(channel))
                    syncManager.initializeState(channel)
                }
                .onFailure { error ->
                    log("TOGETHER join failed: ${error.message}")
                    syncManager.updateChannelState(ChannelState.JoiningFailed(error.message ?: "Unknown error"))
                }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun leaveChannel() {
        scope.launch {
            currentChannelId = null
            webSocketClient.leaveChannel()
            syncManager.updateChannelState(ChannelState.Disconnected)
            _state.value = _state.value.copy(messages = emptyList(), members = emptyList())
        }
    }

    fun sendChatMessage(content: String) {
        scope.launch { webSocketClient.sendChatMessage(content) }
    }

    fun notifyLocalUnsupported() {
        if (localNoticeShown || _state.value.channelState !is ChannelState.Joined) return
        localNoticeShown = true
        addMessage(
            ChannelMessage.System(
                channelId = currentChannelId ?: DEFAULT_CHANNEL_ID,
                sender = User("SYSTEM", "SYSTEM"),
                timestamp = Clock.System.now(),
                content = "本地曲目无法一起听，请切换到在线曲目",
                messageType = SystemMessageType.LEAVE,
            ),
        )
    }

    fun formatTimestamp(timestamp: Instant): String =
        timestamp.toLocalDateTime(TimeZone.of("Asia/Shanghai")).toString().substring(11, 16)

    fun getCurrentUser(): User? {
        val authState = authStore.state.value
        return if (authState is AuthState.Authenticated) {
            User(
                id = authState.user.id,
                username = authState.user.username,
                avatar = authState.user.avatar,
                isOnline = true,
            )
        } else {
            null
        }
    }

    fun isMessageCurrentUser(message: ChannelMessage): Boolean {
        val user = getCurrentUser() ?: return false
        return message is ChannelMessage.Text && message.sender.id == user.id
    }

    fun release() {
        syncManager.cleanup()
        scope.launch { webSocketClient.disconnect() }
        scope.cancel()
    }

    private fun startObserving() {
        scope.launch {
            EventBus.PlaybackEventBus.receive { event ->
                if (event.source != EventSource.User &&
                    event.source != EventSource.System &&
                    event.source != EventSource.Player
                ) return@receive
                if (_state.value.channelState !is ChannelState.Joined) return@receive

                val track = when (event) {
                    is PlaybackEvent.Next -> event.track
                    is PlaybackEvent.Previous -> event.track
                    is PlaybackEvent.TrackChanged -> event.track
                    else -> null
                }
                if (track?.source == TrackSource.LOCAL) {
                    notifyLocalUnsupported()
                    return@receive
                }
                if ((event is PlaybackEvent.PlayStateChanged || event is PlaybackEvent.Seek) &&
                    syncManager.isCurrentTrackLocal()
                ) {
                    notifyLocalUnsupported()
                    return@receive
                }

                scope.launch {
                    when (event) {
                        is PlaybackEvent.PlayStateChanged -> {
                            val command = if (event.isPlaying) {
                                PlayControlEvent.Play(event.position?.milliseconds ?: 0L)
                            } else {
                                PlayControlEvent.Pause(event.position?.milliseconds ?: 0L)
                            }
                            syncManager.sendPlayCommand(command)
                        }
                        is PlaybackEvent.Next -> syncManager.sendPlayCommand(PlayControlEvent.Next(event.track))
                        is PlaybackEvent.Previous -> syncManager.sendPlayCommand(PlayControlEvent.Previous(event.track))
                        is PlaybackEvent.PlayModeChanged ->
                            syncManager.sendPlayCommand(PlayControlEvent.PlayModeChanged(event.playMode))
                        is PlaybackEvent.Seek -> syncManager.sendPlayCommand(PlayControlEvent.Seek(event.position.milliseconds))
                        is PlaybackEvent.TrackChanged ->
                            syncManager.sendPlayCommand(PlayControlEvent.ChangeTrack(event.track, event.isAudioCompleted))
                    }
                }
            }
        }

        scope.launch {
            EventBus.PlayingListEventBus.receive { event ->
                if (event.source != EventSource.User) return@receive
                if (_state.value.channelState !is ChannelState.Joined) return@receive

                scope.launch {
                    when (event) {
                        is PlayingListEvent.TrackAdded ->
                            if (event.track.source == TrackSource.LOCAL) {
                                notifyLocalUnsupported()
                            } else if (syncManager.hasLocalTracks()) {
                                val snapshot = syncManager.collectCurrentState()
                                syncManager.sendPlayCommand(
                                    PlayControlEvent.ChangeTrackList(
                                        snapshot.trackList ?: emptyList(),
                                        snapshot.track,
                                        syncManager.currentQueueRevision(),
                                    ),
                                )
                            } else {
                                syncManager.sendPlayCommand(
                                    PlayControlEvent.QueueChange(
                                        kind = PlayControlEvent.QueueChangeKind.ADD,
                                        track = event.track,
                                        strategy = event.strategy,
                                        baseRevision = syncManager.currentQueueRevision(),
                                    ),
                                )
                            }

                        is PlayingListEvent.TrackListChanged -> {
                            val hasLocal = event.tracks.any { it.source == TrackSource.LOCAL }
                            val shareable = event.tracks.filter { it.source != TrackSource.LOCAL }
                            when {
                                event.tracks.isEmpty() -> syncManager.sendPlayCommand(
                                    PlayControlEvent.ChangeTrackList(
                                        emptyList(),
                                        null,
                                        syncManager.currentQueueRevision(),
                                    ),
                                )
                                shareable.isEmpty() -> notifyLocalUnsupported()
                                else -> {
                                    if (hasLocal) notifyLocalUnsupported()
                                    val play = event.track?.takeIf { it.source != TrackSource.LOCAL }
                                        ?: shareable.first()
                                    syncManager.sendPlayCommand(
                                        PlayControlEvent.ChangeTrackList(
                                            shareable,
                                            play,
                                            syncManager.currentQueueRevision(),
                                        ),
                                    )
                                }
                            }
                        }

                        is PlayingListEvent.TrackRemoved ->
                            if (event.track.source != TrackSource.LOCAL) {
                                syncManager.sendPlayCommand(
                                    PlayControlEvent.QueueChange(
                                        kind = PlayControlEvent.QueueChangeKind.REMOVE,
                                        track = event.track,
                                        baseRevision = syncManager.currentQueueRevision(),
                                    ),
                                )
                            }

                        is PlayingListEvent.TrackMoved ->
                            if (syncManager.hasLocalTracks()) {
                                val snapshot = syncManager.collectCurrentState()
                                syncManager.sendPlayCommand(
                                    PlayControlEvent.ChangeTrackList(
                                        snapshot.trackList ?: emptyList(),
                                        snapshot.track,
                                        syncManager.currentQueueRevision(),
                                    ),
                                )
                            } else {
                                syncManager.sendPlayCommand(
                                    PlayControlEvent.QueueChange(
                                        kind = PlayControlEvent.QueueChangeKind.MOVE,
                                        index = event.from,
                                        toIndex = event.to,
                                        baseRevision = syncManager.currentQueueRevision(),
                                    ),
                                )
                            }
                    }
                }
            }
        }

        scope.launch {
            webSocketClient.connectionStateFlow.collect { connection ->
                syncManager.updateConnectionState(connection)
                _state.value = _state.value.copy(connectionState = connection)
                when (connection) {
                    ConnectionState.CONNECTED -> currentChannelId?.let { joinChannel(it) }
                    ConnectionState.DISCONNECTED -> syncManager.updateChannelState(ChannelState.Disconnected)
                    else -> Unit
                }
            }
        }

        scope.launch {
            webSocketClient.channelEvents().collect { event ->
                when (event) {
                    is ChannelEvent.NewMessage -> addMessage(event.message)
                    is ChannelEvent.UserJoined -> updateMembers(event.members)
                    is ChannelEvent.UserLeft -> updateMembers(event.members)
                    is ChannelEvent.PlayStateChanged -> syncManager.handleRemoteSyncState(event)
                    is ChannelEvent.RoomMetadataChanged -> {
                        updateMembers(event.channel.members)
                        syncManager.initializeState(event.channel)
                    }
                }
            }
        }

        scope.launch {
            webSocketClient.messages().collect { message ->
                addMessage(message)
                _incomingMessages.tryEmit(message)
            }
        }

        scope.launch {
            syncManager.channelState.collect { channel ->
                _state.value = _state.value.copy(channelState = channel)
            }
        }
    }

    private fun addMessage(message: ChannelMessage) {
        val messages = _state.value.messages.toMutableList()
        messages.add(0, message)
        if (messages.size > 100) messages.removeLast()
        _state.value = _state.value.copy(messages = messages)
    }

    private fun updateMembers(members: List<User>) {
        _state.value = _state.value.copy(members = members)
    }
}

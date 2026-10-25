package com.eimsound.eimusic.viewmodel

import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.auth.AuthStore
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.network.TogetherWebSocketClient
import com.eimsound.eimusic.together.ChannelEvent
import com.eimsound.eimusic.together.ChannelMessage
import com.eimsound.eimusic.together.ChannelState
import com.eimsound.eimusic.together.ConnectionState
import com.eimsound.eimusic.together.DEFAULT_CHANNEL_ID
import com.eimsound.eimusic.together.PlayControlEvent
import com.eimsound.eimusic.together.SyncState
import com.eimsound.eimusic.together.TogetherSyncManager
import com.eimsound.eimusic.together.SystemMessageType
import com.eimsound.eimusic.music.TrackSource
import kotlin.time.Clock
import com.eimsound.eimusic.together.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class TogetherUiState(
    val channelState: ChannelState = ChannelState.Disconnected,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val messages: List<ChannelMessage> = emptyList(),
    val members: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val chatInput: String = "",
    val isChatInput: Boolean = false,
    val channelIdInput: String = DEFAULT_CHANNEL_ID,
    val isChannelIdInput: Boolean = false,
)

class ChannelViewModel(
    private val webSocketClient: TogetherWebSocketClient,
    private val syncManager: TogetherSyncManager,
    private val authStore: AuthStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(TogetherUiState())
    val uiState: StateFlow<TogetherUiState> = _uiState.asStateFlow()

    private val _currentChannelId = MutableStateFlow<String?>(null)
    private var localNoticeShown = false

    init { startObserving() }

    fun connect() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                webSocketClient.connect()
            } catch (_: Exception) { }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun disconnect() {
        scope.launch {
            webSocketClient.disconnect()
        }
    }

    fun joinChannel(channelId: String) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val syncState: SyncState = syncManager.collectCurrentState()
            val result = webSocketClient.joinChannel(channelId, syncState)
            result.onSuccess { channel ->
                _currentChannelId.value = channel.id
                localNoticeShown = false
                syncManager.updateChannelState(ChannelState.Joined(channel))
                syncManager.initializeState(channel)
            }.onFailure { error ->
                com.eimsound.eimusic.util.TuiLogger.log("TOGETHER join failed: ${error.message}")
                syncManager.updateChannelState(ChannelState.JoiningFailed(error.message ?: "Unknown error"))
            }
            _uiState.value = _uiState.value.copy(isLoading = false, channelIdInput = "")
        }
    }

    fun leaveChannel() {
        scope.launch {
            _currentChannelId.value = null
            webSocketClient.leaveChannel()
            syncManager.updateChannelState(ChannelState.Disconnected)
            _uiState.value = _uiState.value.copy(
                messages = emptyList(),
                members = emptyList(),
                isChatInput = false,
            )
        }
    }

    fun sendChatMessage(content: String) {
        scope.launch {
            webSocketClient.sendChatMessage(content)
        }
    }

    fun updateChatInput(input: String) {
        _uiState.value = _uiState.value.copy(chatInput = input)
    }

    fun toggleChatInput() {
        _uiState.value = _uiState.value.copy(
            isChatInput = !_uiState.value.isChatInput,
            chatInput = if (_uiState.value.isChatInput) "" else _uiState.value.chatInput,
        )
    }

    fun updateChannelIdInput(input: String) {
        _uiState.value = _uiState.value.copy(channelIdInput = input)
    }

    fun toggleChannelIdInput() {
        _uiState.value = _uiState.value.copy(
            isChannelIdInput = !_uiState.value.isChannelIdInput,
        )
    }

    private fun notifyLocalUnsupported() {
        if (localNoticeShown || _uiState.value.channelState !is ChannelState.Joined) return
        localNoticeShown = true
        addMessage(
            ChannelMessage.System(
                channelId = _currentChannelId.value ?: DEFAULT_CHANNEL_ID,
                sender = User("SYSTEM", "SYSTEM"),
                timestamp = Clock.System.now(),
                content = "本地曲目无法一起听，请切换到在线曲目",
                messageType = SystemMessageType.LEAVE,
            ),
        )
    }

    private fun startObserving() {
        // Observe playback events from local player → forward to WebSocket
        scope.launch {
            EventBus.PlaybackEventBus.receive { event ->
                if (event.source == EventSource.User
                    || event.source == EventSource.System
                    || event.source == EventSource.Player
                ) {
                    if (_uiState.value.channelState !is ChannelState.Joined) return@receive
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
                                val ctl = if (event.isPlaying) {
                                    PlayControlEvent.Play(event.position?.milliseconds ?: 0L)
                                } else {
                                    PlayControlEvent.Pause(event.position?.milliseconds ?: 0L)
                                }
                                syncManager.sendPlayCommand(ctl)
                            }
                            is PlaybackEvent.Next -> syncManager.sendPlayCommand(PlayControlEvent.Next(event.track))
                            is PlaybackEvent.Previous -> syncManager.sendPlayCommand(PlayControlEvent.Previous(event.track))
                            is PlaybackEvent.PlayModeChanged -> syncManager.sendPlayCommand(PlayControlEvent.PlayModeChanged(event.playMode))
                            is PlaybackEvent.Seek -> syncManager.sendPlayCommand(PlayControlEvent.Seek(event.position.milliseconds))
                            is PlaybackEvent.TrackChanged -> syncManager.sendPlayCommand(PlayControlEvent.ChangeTrack(event.track, event.isAudioCompleted))
                        }
                    }
                }
            }
        }

        // Observe playlist events → forward to WebSocket
        scope.launch {
            EventBus.PlayingListEventBus.receive { event ->
                if (event.source == EventSource.User) {
                    if (_uiState.value.channelState !is ChannelState.Joined) return@receive
                    scope.launch {
                        when (event) {
                            is PlayingListEvent.TrackAdded ->
                                if (event.track.source == TrackSource.LOCAL) {
                                    notifyLocalUnsupported()
                                } else {
                                    syncManager.sendPlayCommand(PlayControlEvent.AddToQueue(event.track, event.strategy))
                                }
                            is PlayingListEvent.TrackListChanged -> {
                                val shareable = event.tracks.filter { it.source != TrackSource.LOCAL }
                                if (shareable.isEmpty()) {
                                    notifyLocalUnsupported()
                                } else {
                                    val play = event.track?.takeIf { it.source != TrackSource.LOCAL }
                                        ?: shareable.first()
                                    syncManager.sendPlayCommand(PlayControlEvent.ChangeTrackList(shareable, play))
                                }
                            }
                            is PlayingListEvent.TrackRemoved ->
                                if (event.track.source != TrackSource.LOCAL) {
                                    syncManager.sendPlayCommand(PlayControlEvent.RemoveFromQueue(event.track))
                                }
                        }
                    }
                }
            }
        }

        // WebSocket connection state changes
        scope.launch {
            webSocketClient.connectionStateFlow.collect { state ->
                syncManager.updateConnectionState(state)
                _uiState.value = _uiState.value.copy(connectionState = state)
                when (state) {
                    ConnectionState.CONNECTED -> {
                        _currentChannelId.value?.let { joinChannel(it) }
                    }
                    ConnectionState.DISCONNECTED -> {
                        syncManager.updateChannelState(ChannelState.Disconnected)
                    }
                    else -> {}
                }
            }
        }

        // WebSocket channel events
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

        // WebSocket chat messages
        scope.launch {
            webSocketClient.messages().collect { message ->
                addMessage(message)
            }
        }

        // Update uiState from syncManager's channelState
        scope.launch {
            syncManager.channelState.collect { state ->
                _uiState.value = _uiState.value.copy(channelState = state)
            }
        }
    }

    private fun addMessage(message: ChannelMessage) {
        val current = _uiState.value.messages.toMutableList()
        current.add(0, message)
        if (current.size > 100) current.removeLast()
        _uiState.value = _uiState.value.copy(messages = current)
    }

    private fun updateMembers(members: List<User>) {
        _uiState.value = _uiState.value.copy(members = members)
    }

    fun formatTimestamp(timestamp: Instant): String {
        val chinaTimeZone = TimeZone.of("Asia/Shanghai")
        return timestamp.toLocalDateTime(chinaTimeZone).toString().substring(11, 16)
    }

    fun isMessageCurrentUser(message: ChannelMessage): Boolean {
        val authState = authStore.state.value
        return authState is AuthState.Authenticated
                && message is ChannelMessage.Text
                && message.sender.id == authState.user.id
    }

    fun release() {
        syncManager.cleanup()
        scope.launch { webSocketClient.disconnect() }
        scope.cancel()
    }
}

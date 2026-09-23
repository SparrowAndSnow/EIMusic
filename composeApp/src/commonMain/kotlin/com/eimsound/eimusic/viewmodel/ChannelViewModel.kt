package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.auth.AuthStore
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.media.NotificationService
import com.eimsound.eimusic.network.TogetherWebSocketClient
import com.eimsound.eimusic.together.ChannelEvent
import com.eimsound.eimusic.together.ChannelMessage
import com.eimsound.eimusic.together.ChannelState
import com.eimsound.eimusic.together.ConnectionState
import com.eimsound.eimusic.together.PlayControlEvent
import com.eimsound.eimusic.together.PlayControlEvent.*
import com.eimsound.eimusic.together.SyncState
import com.eimsound.eimusic.together.TogetherSyncManager
import com.eimsound.eimusic.together.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant



/**
 * 频道视图模型
 */
class ChannelViewModel(
    private val webSocketClient: TogetherWebSocketClient,
    private val syncManager: TogetherSyncManager,
    private val authStore: AuthStore,
    private val notificationService: NotificationService
) : ViewModel() {

    val channelState: StateFlow<ChannelState> = syncManager.channelState

    val connectionState: StateFlow<ConnectionState> = syncManager.connectionState

    private val _messages = MutableStateFlow<List<ChannelMessage>>(emptyList())
    val messages: StateFlow<List<ChannelMessage>> = _messages.asStateFlow()


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentChannelId = MutableStateFlow<String?>(null)

    init {
        observeConnectionState()
        observeWebSocketEvents()
        observePlaybackEvents()
        observerPlayListEvents()
    }

    /**
     * 连接到WebSocket服务器
     */
    fun connect() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                webSocketClient.connect()
            } catch (e: Exception) {
                println("连接失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加入频道
     */
    fun joinChannel(channelId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val syncState: SyncState = syncManager.collectCurrentState()
            val result = webSocketClient.joinChannel(channelId, syncState)
            result.onSuccess {
                _currentChannelId.value = it.id
                syncManager.updateChannelState(ChannelState.Joined(it))
            }.onFailure { error ->
                syncManager.updateChannelState(ChannelState.JoiningFailed(error.message ?: "未知错误"))
            }
            _isLoading.value = false
        }
    }

    /**
     * 离开频道
     */
    fun leaveChannel() {
        viewModelScope.launch {
            try {
                _currentChannelId.value = null
                webSocketClient.leaveChannel()
                syncManager.updateChannelState(ChannelState.Disconnected)
                _messages.value = emptyList()
            } catch (e: Exception) {
                // 处理异常
            }
        }
    }

    /**
     * 发送文本消息
     */
    fun sendMessage(content: String) {
        viewModelScope.launch {
            webSocketClient.sendChatMessage(content)
        }
    }

    /**
     * 发送播放控制命令
     */
    fun sendPlayControl(event: PlayControlEvent) {
        viewModelScope.launch {
            syncManager.sendPlayCommand(event)
        }
    }

    /**
     * 观察连接状态变化
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            webSocketClient.connectionStateFlow.collect { state ->
                syncManager.updateConnectionState(state)
                when (state) {
                    ConnectionState.CONNECTED -> {
                        _currentChannelId.value?.let {
                            joinChannel(it)
                        }
                        println("WebSocket已连接")
                    }

                    ConnectionState.DISCONNECTED -> {
                        syncManager.updateChannelState(ChannelState.Disconnected)
                        println("WebSocket已断开")
                    }

                    ConnectionState.CONNECTING -> {
                        println("正在连接WebSocket...")
                    }

                    ConnectionState.RECONNECTING -> {
                        println("正在重连WebSocket...")
                    }
                }
            }
        }
    }

    /**
     * 观察WebSocket事件
     */
    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketClient.channelEvents().collect { event ->
                when (event) {
                    is ChannelEvent.NewMessage -> {
                        addMessage(event.message)
                    }

                    is ChannelEvent.UserJoined -> {
                        updateUserList(event.members)
                    }

                    is ChannelEvent.UserLeft -> {
                        updateUserList(event.members)
                    }

                    is ChannelEvent.PlayStateChanged -> {
                        syncManager.handleRemoteSyncState(event)
                    }

                    is ChannelEvent.RoomMetadataChanged -> {
                        updateUserList(event.channel.members)
                        syncManager.initializeState(event.channel)
                    }

                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            webSocketClient.messages().collect { message ->
                addMessage(message)
                if (!isMessageCurrentUser(message) && message is ChannelMessage.Text) {
                    notificationService.notify(message.sender.username, message.content)
                }
            }
        }
    }

    fun isMessageCurrentUser(message: ChannelMessage): Boolean {
        val currentUser = getCurrentUser()
        return currentUser != null
                && message is ChannelMessage.Text
                && message.sender.id == currentUser.id
    }

    private fun observePlaybackEvents() {
        viewModelScope.launch {
            EventBus.PlaybackEventBus.receive {
                if (it.source == EventSource.User
                    || it.source == EventSource.System
                    || it.source == EventSource.Player
                ) when (it) {
                    is PlaybackEvent.PlayStateChanged -> {
                        if (it.isPlaying) {
                            sendPlayControl(Play(it.position?.milliseconds ?: syncManager.position))
                        } else {
                            sendPlayControl(Pause(it.position?.milliseconds ?: syncManager.position))
                        }
                    }

                    is PlaybackEvent.Next -> {
                        sendPlayControl(Next(it.track))
                    }

                    is PlaybackEvent.Previous -> {
                        sendPlayControl(Previous(it.track))
                    }

                    is PlaybackEvent.PlayModeChanged -> {
                        sendPlayControl(PlayModeChanged(it.playMode))
                    }

                    is PlaybackEvent.Seek -> {
                        sendPlayControl(Seek(it.position.milliseconds))
                    }

                    is PlaybackEvent.TrackChanged -> {
                        sendPlayControl(ChangeTrack(it.track, it.isAudioCompleted))
                    }
                }
            }
        }
    }

    private fun observerPlayListEvents() {
        viewModelScope.launch {
            EventBus.PlayingListEventBus.receive {
                if (it.source == EventSource.User) when (it) {
                    is PlayingListEvent.TrackAdded -> {
                        sendPlayControl(AddToQueue(it.track, it.strategy))
                    }

                    is PlayingListEvent.TrackListChanged -> {
                        sendPlayControl(ChangeTrackList(it.tracks, it.track))
                    }

                    is PlayingListEvent.TrackRemoved -> {
                        sendPlayControl(RemoveFromQueue(it.track))
                    }
                }
            }
        }
    }

    /**
     * 添加新消息
     */
    private fun addMessage(message: ChannelMessage) {
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(0, message) // 最新消息在前面
        // 限制消息数量
        if (currentMessages.size > 100) {
            currentMessages.removeLast()
        }
        _messages.value = currentMessages
    }

    /**
     * 更新用户列表
     */
    private fun updateUserList(members: List<User>) {
        viewModelScope.launch {
            val state = channelState.value
            when (state) {
                is ChannelState.Joined -> {
                    syncManager.updateChannelState(ChannelState.Joined(state.channel.copy(members = members)))
                }

                else -> {}
            }
        }
    }


    /**
     * 获取格式化的时间字符串
     */
    fun formatTimestamp(timestamp: Instant): String {
        val chinaTimeZone = TimeZone.of("Asia/Shanghai")
        return timestamp.toLocalDateTime(chinaTimeZone).toString().substring(11, 16) // HH:mm 格式
    }

    /**
     * 获取当前登录用户
     */
    fun getCurrentUser(): User? {
        val authState = authStore.state.value
        return if (authState is AuthState.Authenticated) {
            User(
                id = authState.user.id,
                username = authState.user.username,
                avatar = authState.user.avatar,
                isOnline = true
            )
        } else {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncManager.cleanup()
        viewModelScope.launch {
            webSocketClient.disconnect()
        }
    }
}
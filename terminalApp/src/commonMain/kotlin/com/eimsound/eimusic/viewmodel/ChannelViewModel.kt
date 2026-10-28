package com.eimsound.eimusic.viewmodel

import com.eimsound.eimusic.together.ChannelMessage
import com.eimsound.eimusic.together.ChannelState
import com.eimsound.eimusic.together.ConnectionState
import com.eimsound.eimusic.together.DEFAULT_CHANNEL_ID
import com.eimsound.eimusic.together.TogetherStore
import com.eimsound.eimusic.together.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

/** terminalApp 的薄适配层：只保留输入框状态 + TogetherStore。 */
class ChannelViewModel(
    private val store: TogetherStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(TogetherUiState())
    val uiState: StateFlow<TogetherUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            store.state.collect { state ->
                _uiState.update {
                    it.copy(
                        channelState = state.channelState,
                        connectionState = state.connectionState,
                        messages = state.messages,
                        members = state.members,
                        isLoading = state.isLoading,
                    )
                }
            }
        }
    }

    fun connect() = store.connect()

    fun disconnect() = store.disconnect()

    fun joinChannel(channelId: String) {
        store.joinChannel(channelId)
        _uiState.update { it.copy(channelIdInput = "") }
    }

    fun leaveChannel() {
        store.leaveChannel()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                members = emptyList(),
                isChatInput = false,
            )
        }
    }

    fun sendChatMessage(content: String) = store.sendChatMessage(content)

    fun updateChatInput(input: String) {
        _uiState.update { it.copy(chatInput = input) }
    }

    fun setChatInput(active: Boolean) {
        _uiState.update { it.copy(isChatInput = active) }
    }

    fun toggleChatInput() {
        _uiState.update {
            it.copy(
                isChatInput = !it.isChatInput,
                chatInput = if (it.isChatInput) "" else it.chatInput,
            )
        }
    }

    fun updateChannelIdInput(input: String) {
        _uiState.update { it.copy(channelIdInput = input) }
    }

    fun toggleChannelIdInput() {
        _uiState.update { it.copy(isChannelIdInput = !it.isChannelIdInput) }
    }

    fun notifyLocalUnsupported() = store.notifyLocalUnsupported()

    fun formatTimestamp(timestamp: Instant): String =
        timestamp.toLocalDateTime(TimeZone.of("Asia/Shanghai")).toString().substring(11, 16)

    fun isMessageCurrentUser(message: ChannelMessage): Boolean = store.isMessageCurrentUser(message)

    fun release() {
        store.release()
        scope.cancel()
    }
}

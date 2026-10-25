package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.media.NotificationService
import com.eimsound.eimusic.together.ChannelMessage
import com.eimsound.eimusic.together.ChannelState
import com.eimsound.eimusic.together.ConnectionState
import com.eimsound.eimusic.together.TogetherStore
import com.eimsound.eimusic.together.User
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** composeApp 的薄适配层；连接/频道/队列逻辑在共享 TogetherStore。 */
class ChannelViewModel(
    private val store: TogetherStore,
    private val notificationService: NotificationService,
) : ViewModel() {

    val channelState: StateFlow<ChannelState> = store.state
        .map { it.channelState }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ChannelState.Disconnected)

    val connectionState: StateFlow<ConnectionState> = store.state
        .map { it.connectionState }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.DISCONNECTED)

    val messages: StateFlow<List<ChannelMessage>> = store.state
        .map { it.messages }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isLoading: StateFlow<Boolean> = store.state
        .map { it.isLoading }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            store.incomingMessages.collect { message ->
                if (!isMessageCurrentUser(message) && message is ChannelMessage.Text) {
                    notificationService.notify(message.sender.username, message.content)
                }
            }
        }
    }

    fun connect() = store.connect()

    fun joinChannel(channelId: String) = store.joinChannel(channelId)

    fun leaveChannel() = store.leaveChannel()

    fun sendMessage(content: String) = store.sendChatMessage(content)

    fun notifyLocalUnsupported() = store.notifyLocalUnsupported()

    fun formatTimestamp(timestamp: Instant): String =
        timestamp.toLocalDateTime(TimeZone.of("Asia/Shanghai")).toString().substring(11, 16)

    fun getCurrentUser(): User? = store.getCurrentUser()

    fun isMessageCurrentUser(message: ChannelMessage): Boolean = store.isMessageCurrentUser(message)

    override fun onCleared() {
        super.onCleared()
        store.disconnect()
    }
}

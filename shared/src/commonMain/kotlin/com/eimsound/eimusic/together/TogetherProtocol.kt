package com.eimsound.eimusic.together

import kotlinx.serialization.SerialName
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
sealed interface TogetherProtocol {
    val timestamp: Instant

    // --- 客户端 -> 服务端：动作 (Actions) ---

    @Serializable
    @SerialName("act_join")
    data class JoinChannel(
        val channelId: String,
        val syncState: SyncState,
        override val timestamp: Instant = Clock.System.now()
    ) :
        TogetherProtocol

    @Serializable
    @SerialName("act_leave")
    data class LeaveChannel(val channelId: String, override val timestamp: Instant = Clock.System.now()) :
        TogetherProtocol

    @Serializable
    @SerialName("act_chat")
    data class SendChat(
        val channelId: String,
        val content: String,
        override val timestamp: Instant = Clock.System.now()
    ) : TogetherProtocol

    @Serializable
    @SerialName("act_control")
    data class PlayControlAction(
        val channelId: String,
        val event: PlayControlEvent,
        val commandId: String? = null,
        override val timestamp: Instant = Clock.System.now()
    ) : TogetherProtocol

    @Serializable
    @SerialName("act_heartbeat")
    data class Heartbeat(override val timestamp: Instant = Clock.System.now()) : TogetherProtocol

    // --- 服务端 -> 客户端：推送 (Server Response) ---

    @Serializable
    @SerialName("res_event")
    data class ServerEvent(val event: ChannelEvent, override val timestamp: Instant = Clock.System.now()) :
        TogetherProtocol

    @Serializable
    @SerialName("res_message")
    data class MessageReceived(val message: ChannelMessage, override val timestamp: Instant = Clock.System.now()) :
        TogetherProtocol

    @Serializable
    @SerialName("res_error")
    data class ErrorResponse(
        val message: String,
        val code: Int = 500,
        override val timestamp: Instant = Clock.System.now()
    ) : TogetherProtocol
}

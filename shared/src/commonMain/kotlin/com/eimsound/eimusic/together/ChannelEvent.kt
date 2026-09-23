package com.eimsound.eimusic.together

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 频道事件类型
 */
@Serializable
sealed interface ChannelEvent {
    val channelId: String

    /**
     * 用户加入频道
     */
    @SerialName("evt_user_joined")
    @Serializable
    data class UserJoined(
        override val channelId: String,
        val user: User,
        val members: List<User>
    ) : ChannelEvent

    /**
     * 用户离开频道
     */
    @SerialName("evt_user_left")
    @Serializable
    data class UserLeft(
        override val channelId: String,
        val user: User,
        val members: List<User>
    ) : ChannelEvent

    /**
     * 播放状态变更
     */
    @SerialName("evt_play_state_changed")
    @Serializable
    data class PlayStateChanged(
        override val channelId: String,
        val event: PlayControlEvent,
        val state: SyncState
    ) : ChannelEvent

    /**
     * 新消息
     */
    @SerialName("evt_new_message")
    @Serializable
    data class NewMessage(
        override val channelId: String,
        val message: ChannelMessage
    ) : ChannelEvent

    @Serializable
    @SerialName("evt_room_meta")
    data class RoomMetadataChanged(override val channelId: String, val channel: Channel) : ChannelEvent
}
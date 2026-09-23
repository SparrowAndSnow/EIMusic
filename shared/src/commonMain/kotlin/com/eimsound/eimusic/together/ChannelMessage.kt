package com.eimsound.eimusic.together

import kotlinx.serialization.SerialName
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Clock

/**
 * 频道消息类型
 */
@Serializable
sealed interface ChannelMessage {
    val channelId: String
    val sender: User      // 发送者信息（系统消息时通常为 SYSTEM 用户）
    val isShow: Boolean
    val timestamp: Instant

    /**
     * 用户发送的纯文本聊天
     */
    @Serializable
    @SerialName("msg_text")
    data class Text(
        override val channelId: String,
        override val sender: User,
        override val timestamp: Instant = Clock.System.now(),
        val content: String,
        override val isShow: Boolean = true
    ) : ChannelMessage

    /**
     * 系统自动生成的通知（进入、退出、踢人、房间更新）
     */
    @Serializable
    @SerialName("msg_system")
    data class System(
        override val channelId: String,
        override val sender: User, // 通常是固定的系统虚拟用户
        override val timestamp: Instant = Clock.System.now(),
        val content: String,
        val messageType: SystemMessageType,
        override val isShow: Boolean = true
    ) : ChannelMessage

    /**
     * 播放操作记录（用于在聊天列表中显示：“XX 暂停了音乐”）
     */
    @Serializable
    @SerialName("msg_control_log")
    data class PlayControl(
        override val channelId: String,
        override val sender: User,
        override val timestamp: Instant = Clock.System.now(),
        val event: PlayControlEvent,
        override val isShow: Boolean = true
    ) : ChannelMessage
}

/**
 * 将播放控制动作转换为对应的播放状态
 */
fun PlayControlType.toPlayState(): PlayState {
    return when (this) {
        PlayControlType.PLAY -> PlayState.PLAYING
        PlayControlType.PAUSE -> PlayState.PAUSED
        PlayControlType.STOP -> PlayState.STOPPED
        PlayControlType.SEEK -> PlayState.PLAYING         // 跳转进度通常意味着继续播放
        PlayControlType.NEXT -> PlayState.PLAYING         // 切歌后自动开始播放
        PlayControlType.PREVIOUS -> PlayState.PLAYING     // 同上
        PlayControlType.CHANGE_TRACK -> PlayState.PLAYING // 同上
        PlayControlType.ADD_TO_QUEUE -> PlayState.PLAYING // 逻辑依业务而定
        PlayControlType.CHANGE_TRACK_LIST -> PlayState.PLAYING
    }
}

/**
 * 播放控制类型
 */
@Serializable
enum class PlayControlType {
    PLAY,
    PAUSE,
    STOP,
    SEEK,
    NEXT,
    PREVIOUS,
    CHANGE_TRACK,
    ADD_TO_QUEUE,
    CHANGE_TRACK_LIST,
}

/**
 * 系统消息类型
 */
@Serializable
enum class SystemMessageType {
    JOIN,
    LEAVE,
    KICK,
    CHANNEL_CREATED,
    CHANNEL_DELETED
}
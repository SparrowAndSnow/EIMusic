package com.eimsound.eimusic.together

import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.Track
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Clock

val DEFAULT_CHANNEL_ID = "default"

/**
 * 连接状态枚举
 */
enum class ConnectionState {
    DISCONNECTED,    // 已断开
    CONNECTING,      // 连接中
    CONNECTED,       // 已连接
    RECONNECTING     // 重连中
}

/**
 * 频道信息数据模型
 */
@Serializable
data class Channel(
    val id: String,
    val name: String,
    val description: String = "",
    val owner: User,
    val members: List<User> = emptyList(),
    val currentTrack: Track? = null,
    val trackList: List<Track> = emptyList(),
    val playMode: PlayMode = PlayMode.Loop,
    val playState: PlayState = PlayState.STOPPED,
    val currentPosition: Long = 0L, // 当前播放位置（毫秒）
    val queueRevision: Long = 0L, // 队列快照版本，用于并发 CAS
    // 播放锚点时间戳
    val anchorTimestamp: Instant = Clock.System.now(),
    val createdAt: Instant = Clock.System.now(),
    val maxMembers: Int = 50,
    val isPrivate: Boolean = false
)

/**
 * 用户信息数据模型
 */
@Serializable
data class User(
    val id: String,
    val username: String,
    val avatar: String? = null,
    val isOnline: Boolean = true,
    val joinTime: Instant = Clock.System.now(),
)

/**
 * 播放状态枚举
 */
@Serializable
enum class PlayState {
    PLAYING,
    PAUSED,
    STOPPED
}

val PlayState.isPlaying
    get() = this == PlayState.PLAYING


/**
 * 播放同步状态
 */
@Serializable
data class SyncState(
    val track: Track? = null,
    val trackList: List<Track>? = null,
    val queueRevision: Long = 0L,
    val playMode: PlayMode? = null,
    val playState: PlayState? = null,
    val position: Long? = null,
    val anchorTimestamp: Instant? = null,
    val timestamp: Instant = Clock.System.now(),
    val controlledBy: User? = null
)

@Serializable
enum class MessageType {
    JOIN_CHANNEL,
    LEAVE_CHANNEL,
    SEND_MESSAGE,
    PLAY_CONTROL,
    SYNC_REQUEST,
    SYNC_RESPONSE,
    HEARTBEAT,
    ERROR
}

/**
 * 将完整的频道对象转换为精简的同步快照
 */
fun Channel.toSyncState(): SyncState {
    return SyncState(
        track = currentTrack,       // 当前正在播放的歌曲
        playState = playState,      // 当前状态 (PLAYING/PAUSED)
        position = currentPosition, // 当前播放进度（毫秒）
        timestamp = Clock.System.now(),  // 关键：捕捉生成快照这一刻的服务器绝对时间
        controlledBy = owner       // 默认记录为房主，或记录最后一次操作者
    )
}
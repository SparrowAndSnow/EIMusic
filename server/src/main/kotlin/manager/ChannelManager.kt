package com.eimsound.eimusic.server.manager

import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.server.model.SYSTEM
import com.eimsound.eimusic.server.model.ServerChannel
import com.eimsound.eimusic.server.model.UserSession
import com.eimsound.eimusic.together.Channel
import com.eimsound.eimusic.together.ChannelEvent
import com.eimsound.eimusic.together.ChannelMessage
import com.eimsound.eimusic.together.CreateChannelResult
import com.eimsound.eimusic.together.JoinChannelResult
import com.eimsound.eimusic.together.LeaveChannelResult
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import com.eimsound.eimusic.together.SystemMessageType
import com.eimsound.eimusic.together.TogetherProtocol
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.time.Clock

/**
 * 频道管理器
 * 负责频道的创建、维护、清理等操作
 */
sealed interface QueueUpdateResult {
    data class Success(val state: SyncState) : QueueUpdateResult
    data class Conflict(val channel: Channel) : QueueUpdateResult
    data object NotFound : QueueUpdateResult
}
class ChannelManager(
    private val maxChannels: Int = 1000, private val maxUsersPerChannel: Int = 50
) {
    private val logger = LoggerFactory.getLogger(ChannelManager::class.java)
    private val mutex = Mutex()

    // 频道存储 channel_id -> ServerChannel
    private val channels = mutableMapOf<String, ServerChannel>()

    /**
     * 创建新频道
     */
    suspend fun createChannel(
        channelId: String,
        channelName: String,
        owner: UserSession,
        syncState: SyncState = SyncState(),
        description: String = ""
    ): CreateChannelResult = mutex.withLock {
        // 检查频道数量限制
        if (channels.size >= maxChannels) {
            logger.warn("Maximum channels reached: $maxChannels")
            return CreateChannelResult.LimitExceeded
        }

        // 检查频道是否已存在
        if (channels.containsKey(channelId)) {
            logger.warn("Channel $channelId already exists")
            val channel = channels[channelId]?.channel ?: return CreateChannelResult.Error("Channel not found")
            return CreateChannelResult.Exists(channel)
        }

        val channel = Channel(
            id = channelId,
            name = channelName,
            description = description,
            owner = owner.user,
            members = listOf(owner.user),
            playState = syncState.playState ?: PlayState.STOPPED,
            currentPosition = syncState.position ?: 0L,
            trackList = syncState.trackList ?: emptyList(),
            currentTrack = syncState.track ?: null,
            playMode = syncState.playMode ?: PlayMode.Loop,
            createdAt = Clock.System.now(),
            maxMembers = maxUsersPerChannel,
            isPrivate = false
        )

        val serverChannel = ServerChannel(
            channel = channel,
            createTime = Clock.System.now(),
            lastActivity = Clock.System.now(),
            owner = owner,
            members = mutableSetOf(owner)
        )

        channels[channelId] = serverChannel

        logger.info("Created channel $channelId named '$channelName' by user ${owner.user.username}")
        return CreateChannelResult.Success(channel)
    }

    /**
     * 获取频道信息
     */
    suspend fun getChannel(channelId: String): Channel? = mutex.withLock {
        channels[channelId]?.channel
    }

    /**
     * 用户加入频道
     */
    suspend fun joinChannel(channelId: String, userSession: UserSession): JoinChannelResult = mutex.withLock {
        val serverChannel = channels[channelId] ?: return JoinChannelResult.NotFound

        // 检查频道人数限制
        if (serverChannel.channel.members.size >= serverChannel.channel.maxMembers) {
            logger.warn("Channel $channelId is full (max: ${serverChannel.channel.maxMembers})")
            return JoinChannelResult.ChannelFull
        }

        // 检查用户是否已在频道中
        if (serverChannel.channel.members.any { it.id == userSession.user.id }) {
            logger.debug("User ${userSession.user.username} is already in channel $channelId")
            return JoinChannelResult.AlreadyInChannel(serverChannel.channel)
        }

        serverChannel.addSession(userSession)

        logger.info("User ${userSession.user.username} joined channel $channelId")
        return JoinChannelResult.Success(serverChannel.channel)
    }

    /**
     * 用户离开频道
     */
    suspend fun leaveChannel(channelId: String?, userSession: UserSession): LeaveChannelResult = mutex.withLock {
        val serverChannel = channels[channelId] ?: return LeaveChannelResult.NotFound

        if (serverChannel.members.none { it.sessionId == userSession.sessionId }) {
            logger.warn("User ${userSession.user.id} is not in channel $channelId")
            return LeaveChannelResult.NotInChannel
        }
        // 移除用户
        serverChannel.removeSession(userSession)

        logger.info("User ${userSession.user.id} left channel $channelId")

        // 如果频道变空，标记为待清理
        if (serverChannel.members.isEmpty()) {
            return LeaveChannelResult.SuccessAndChannelEmpty(serverChannel.channel)
        }
        return LeaveChannelResult.Success(serverChannel.channel)
    }

    /**
     * 更新频道播放状态
     */
    suspend fun updatePlayState(
        channelId: String,
        block: (Channel) -> SyncState?,
    ): SyncState? = mutex.withLock {
        val serverChannel = channels[channelId] ?: return null
        val channel = serverChannel.channel

        val state = block(channel) ?: return null

        val queueChanged = state.trackList != null
        val updatedChannel = channel.copy(
            currentTrack = state.track ?: channel.currentTrack,
            trackList = state.trackList ?: channel.trackList,
            playState = state.playState ?: channel.playState,
            playMode = state.playMode ?: channel.playMode,
            currentPosition = state.position ?: channel.currentPosition,
            anchorTimestamp = state.anchorTimestamp ?: channel.anchorTimestamp,
            queueRevision = channel.queueRevision + if (queueChanged) 1 else 0,
        )

        serverChannel.channel = updatedChannel
        serverChannel.lastActivity = Clock.System.now()

        return state.copy(queueRevision = updatedChannel.queueRevision)
    }

    /**
     * 原子地应用队列快照：校验 baseRevision 后再递增，避免并发覆盖。
     * 空列表表示清空队列，此时必须同时清空 currentTrack 并停止播放。
     */
    suspend fun updateQueue(
        channelId: String,
        expectedRevision: Long?,
        block: (Channel) -> SyncState?,
    ): QueueUpdateResult = mutex.withLock {
        val serverChannel = channels[channelId] ?: return@withLock QueueUpdateResult.NotFound
        val channel = serverChannel.channel
        if (expectedRevision != null && expectedRevision != channel.queueRevision) {
            return@withLock QueueUpdateResult.Conflict(channel)
        }

        val state = block(channel) ?: return@withLock QueueUpdateResult.NotFound
        val clearing = state.trackList?.isEmpty() == true
        val now = Clock.System.now()
        val updatedChannel = channel.copy(
            currentTrack = when {
                clearing -> null
                state.track != null -> state.track
                else -> channel.currentTrack
            },
            trackList = state.trackList ?: channel.trackList,
            playState = if (clearing) PlayState.STOPPED else state.playState ?: channel.playState,
            currentPosition = if (clearing) 0L else state.position ?: channel.currentPosition,
            anchorTimestamp = state.anchorTimestamp ?: now,
            queueRevision = channel.queueRevision + 1,
        )
        serverChannel.channel = updatedChannel
        serverChannel.lastActivity = now

        QueueUpdateResult.Success(
            state.copy(
                track = if (clearing) null else state.track,
                playState = if (clearing) PlayState.STOPPED else state.playState,
                position = if (clearing) 0L else state.position,
                queueRevision = updatedChannel.queueRevision,
            ),
        )
    }

    /**
     * 更新频道房主逻辑
     */
    suspend fun updateOwner(channelId: String, newOwner: UserSession): Boolean = mutex.withLock {
        val serverChannel = channels[channelId] ?: return@withLock false

        if (serverChannel.members.none { it.sessionId == newOwner.sessionId }) {
            logger.warn("User ${newOwner.user.id} is not in channel $channelId, cannot promote to owner")
            return@withLock false
        }

        serverChannel.changeOwner(newOwner)

        logger.info("Channel $channelId owner updated to ${newOwner.user.username}")
        true
    }

    /** 房主离开后，在同一把锁内选出新房主，避免把内部可变对象暴露给调用方。 */
    suspend fun promoteNextOwner(channelId: String, oldOwnerId: String): UserSession? = mutex.withLock {
        val serverChannel = channels[channelId] ?: return@withLock null
        if (serverChannel.owner.user.id != oldOwnerId) return@withLock null
        val newOwner = serverChannel.members.firstOrNull() ?: return@withLock null
        serverChannel.changeOwner(newOwner)
        logger.info("Channel $channelId owner updated to ${newOwner.user.username}")
        newOwner
    }

    /**
     * 获取频道统计信息
     */
    suspend fun getChannelStats(channelId: String): Map<String, Any?>? = mutex.withLock {
        val serverChannel = channels[channelId] ?: return@withLock null

        mapOf(
            "channelId" to channelId,
            "memberCount" to serverChannel.channel.members.size,
            "onlineCount" to serverChannel.members.size,
            "playState" to serverChannel.channel.playState.name,
            "currentTrack" to serverChannel.channel.currentTrack,
            "createTime" to serverChannel.createTime.toString(),
            "lastActivity" to serverChannel.lastActivity.toString()
        )
    }

    /**
     * 获取所有频道列表
     */
    suspend fun getAllChannels(): List<Channel> = mutex.withLock {
        channels.values.map { it.channel }
    }


    /**
     * 清理不活跃的频道
     */
    suspend fun cleanupInactiveChannels(sessionManager: UserSessionManager, timeoutSeconds: Long = 30): Int {
        // 会话快照在频道锁外获取，避免长时间持有频道锁等待 session 锁。
        val activeSessionIds = sessionManager.getAllSessions().map { it.sessionId }.toSet()

        val (cleanedCount, broadcasts) = mutex.withLock {
            val pending = mutableListOf<PendingBroadcast>()

            channels.values.forEach { channel ->
                val (activeSessions, expiredSessions) = channel.members.partition { member ->
                    member.sessionId in activeSessionIds
                }
                if (expiredSessions.isEmpty()) return@forEach

                channel.removeSessions(expiredSessions)

                val expiredUserNameString = expiredSessions.joinToString(", ") { it.user.username }
                logger.info("Cleaned up inactive channel ${channel.channel.id} members: $expiredUserNameString")

                val currentUsers = activeSessions.map { it.user }
                val channelId = channel.channel.id

                val ownerStillPresent = activeSessions.any { it.user.id == channel.channel.owner.id }
                if (!ownerStillPresent) {
                    activeSessions.firstOrNull()?.let { newOwner ->
                        channel.changeOwner(newOwner)
                        pending += PendingBroadcast(
                            channelId,
                            TogetherProtocol.MessageReceived(
                                ChannelMessage.System(
                                    channelId,
                                    SYSTEM,
                                    content = "${newOwner.user.username} 自动成为新房主",
                                    messageType = SystemMessageType.JOIN,
                                ),
                            ),
                        )
                    }
                }

                pending += PendingBroadcast(
                    channelId,
                    TogetherProtocol.ServerEvent(
                        ChannelEvent.UserLeft(channelId, expiredSessions[0].user, currentUsers),
                    ),
                )
                pending += PendingBroadcast(
                    channelId,
                    TogetherProtocol.MessageReceived(
                        ChannelMessage.System(
                            channelId,
                            SYSTEM,
                            content = "$expiredUserNameString 已断开",
                            messageType = SystemMessageType.LEAVE,
                        ),
                    ),
                )
            }

            val inactiveChannels = channels.filter { (_, serverChannel) ->
                serverChannel.isInactive(timeoutSeconds)
            }
            inactiveChannels.forEach { (channelId, _) ->
                channels.remove(channelId)
                logger.info("Cleaned up inactive channel: $channelId")
            }

            if (inactiveChannels.isNotEmpty()) {
                logger.info("Cleaned up ${inactiveChannels.size} inactive channels")
            }
            inactiveChannels.size to pending
        }

        // 广播放到频道锁外，避免网络发送阻塞频道管理。
        broadcasts.forEach { pending ->
            runCatching { sessionManager.broadcast(pending.channelId, pending.message) }
                .onFailure { logger.warn("Cleanup broadcast failed for ${pending.channelId}: ${it.message}") }
        }
        return cleanedCount
    }

    private data class PendingBroadcast(
        val channelId: String,
        val message: TogetherProtocol,
    )

    /**
     * 获取系统统计信息
     */
    suspend fun getSystemStats(): Map<String, Any> = mutex.withLock {
        val totalChannels = channels.size
        val totalMembers = channels.values.sumOf { it.channel.members.size }
        val onlineUsers = channels.values.sumOf { it.members.size }
        val emptyChannels = channels.count { it.value.channel.members.isEmpty() }

        mapOf(
            "totalChannels" to totalChannels,
            "totalMembers" to totalMembers,
            "onlineUsers" to onlineUsers,
            "emptyChannels" to emptyChannels,
            "maxChannels" to maxChannels,
            "avgMembersPerChannel" to if (totalChannels > 0) totalMembers.toDouble() / totalChannels else 0.0
        )
    }

    suspend fun deleteChannel(channelId: String) = mutex.withLock {
        channels.remove(channelId)
        Unit
    }
}
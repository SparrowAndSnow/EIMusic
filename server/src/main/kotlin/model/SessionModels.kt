package com.eimsound.eimusic.server.model

import com.eimsound.eimusic.together.Channel
import com.eimsound.eimusic.together.User
import io.ktor.server.websocket.DefaultWebSocketServerSession
import kotlinx.serialization.Serializable
import kotlin.collections.plus
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

val SYSTEM = User("SYSTEM", "SYSTEM", "SYSTEM")

/**
 * 服务器端的用户会话信息
 */
@Serializable
data class UserSession(
    val sessionId: String,
    val session: DefaultWebSocketServerSession,
    val user: User,
    val connectTime: Instant,
    var lastHeartbeat: Instant = Instant.DISTANT_PAST,
    var currentChannelId: String? = null,
) : DefaultWebSocketServerSession by session

/**
 * 服务器端的频道信息扩展
 */
@Serializable
data class ServerChannel(
    var channel: Channel,
    var owner: UserSession,
    val createTime: Instant,
    var lastActivity: Instant = Instant.DISTANT_PAST,
    val members: MutableSet<UserSession> = mutableSetOf()
) {
    /**
     * 添加活跃会话
     */
    fun addSession(userSession: UserSession) {
        members.add(userSession)

        val updatedMembers = channel.members + userSession.user
        channel = channel.copy(
            members = updatedMembers
        )

        lastActivity = Clock.System.now()
    }

    /**
     * 移除活跃会话
     */
    fun removeSession(userSession: UserSession) {
        members.removeIf { it.sessionId == userSession.sessionId }

        val updatedMembers = channel.members.filter { it.id != userSession.user.id }
        channel = channel.copy(
            members = updatedMembers
        )

        lastActivity = Clock.System.now()
    }

    fun removeSessions(userSessions: List<UserSession>) {
        val sessionIdsToRemove = userSessions.map { it.sessionId }.toSet()

        members.removeIf { member -> member.sessionId in sessionIdsToRemove }

        channel = channel.copy(
            members = members.map { it.user }
        )

        lastActivity = Clock.System.now()
    }

    /**
     * 检查是否为空闲频道
     */
    fun isInactive(timeoutSeconds: Long): Boolean {
        return members.isEmpty() &&
                Clock.System.now().minus(timeoutSeconds.seconds) > lastActivity
    }


    fun changeOwner(newOwner: UserSession) {
        owner = newOwner
        channel = channel.copy(
            owner = newOwner.user
        )
    }


    /**
     * 获取在线用户数量
     */
    fun getOnlineUserCount(): Int = members.size
}
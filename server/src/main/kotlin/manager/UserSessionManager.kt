package com.eimsound.eimusic.server.manager

import com.eimsound.eimusic.server.model.UserSession
import com.eimsound.eimusic.together.TogetherProtocol
import com.eimsound.eimusic.together.User
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 用户会话管理器
 * 负责管理所有 WebSocket 连接的用户会话
 */
class UserSessionManager(
    private val maxConnections: Int = 10000,
    // 心跳超时时间设置为心跳间隔的 3.5 倍，避免网络波动导致误判
    private val heartbeatTimeout: Duration = 105.seconds
) {
    private val logger = LoggerFactory.getLogger(UserSessionManager::class.java)
    private val mutex = Mutex()

    private val sessions = mutableMapOf<String, UserSession>()
    private val userToSession = mutableMapOf<String, String>()
    private val channelSessions = mutableMapOf<String, MutableSet<String>>()

    // --- Public API (带锁保护) ---

    suspend fun createSession(
        session: DefaultWebSocketServerSession,
        user: User,
        forceCleanup: Boolean = true
    ): CreateUserSessionResult = mutex.withLock {
        if (sessions.size >= maxConnections) {
            logger.warn("Maximum connections reached: $maxConnections")
            return CreateUserSessionResult.LimitExceeded
        }

        // 处理重复登录
        userToSession[user.id]?.let { existingId ->
            if (forceCleanup) {
                sessions[existingId]?.let { cleanupInternal(it) }
            } else {
                return CreateUserSessionResult.Exists
            }
        }

//        val sessionId = generateSessionId(session)
        val userSession = UserSession(
            sessionId = user.id,
            user = user.copy(isOnline = true),
            session = session,
            connectTime = Clock.System.now(),
            lastHeartbeat = Clock.System.now()
        )

        addSessionInternal(userSession)
        return CreateUserSessionResult.Success(userSession)
    }

    suspend fun updateHeartbeat(sessionId: String) = mutex.withLock {
        sessions[sessionId]?.apply {
            lastHeartbeat = Clock.System.now()
        }
    }

    suspend fun joinChannel(sessionId: String, channelId: String) = mutex.withLock {
        val session = sessions[sessionId] ?: return@withLock
        // 离开旧频道
        session.currentChannelId?.let { leaveChannelInternal(sessionId, it) }
        // 加入新频道
        joinChannelInternal(sessionId, channelId)
    }

    suspend fun leaveChannel(sessionId: String, channelId: String) = mutex.withLock {
        leaveChannelInternal(sessionId, channelId)
    }

    suspend fun removeSession(sessionId: String): UserSession? = mutex.withLock {
        val session = sessions[sessionId] ?: return@withLock null
        cleanupInternal(session)
        return@withLock session
    }

    /**
     * 健康检查：清理过期会话
     */
    suspend fun checkConnectionHealth() {
        val toRemove = mutableListOf<UserSession>()
        val now = Clock.System.now()

        mutex.withLock {
            sessions.values.forEach {
                if (now - it.lastHeartbeat > heartbeatTimeout) toRemove.add(it)
            }
            toRemove.forEach { cleanupInternal(it) }
        }

        // 在锁外关闭网络连接，避免阻塞
        toRemove.forEach { it.session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Heartbeat timeout")) }
        if (toRemove.isNotEmpty()) logger.info("Cleaned up ${toRemove.size} expired sessions")
    }

    suspend fun closeAllSessions() {
        val allSessions = mutex.withLock {
            val copy = sessions.values.toList()
            copy.forEach { cleanupInternal(it) }
            copy
        }
        allSessions.forEach { it.session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server shutting down")) }
    }

    suspend fun getAllSessions(): List<UserSession> = mutex.withLock {
        sessions.values.toList()
    }

    private fun addSessionInternal(userSession: UserSession) {
        sessions[userSession.sessionId] = userSession
        userToSession[userSession.user.id] = userSession.sessionId
        logger.info("Session created: ${userSession.sessionId} for user ${userSession.user.id}")
    }

    private fun leaveChannelInternal(sessionId: String, channelId: String) {
        sessions[sessionId]?.currentChannelId = null
        channelSessions[channelId]?.remove(sessionId)
        if (channelSessions[channelId].isNullOrEmpty()) {
            channelSessions.remove(channelId)
        }
    }

    private fun joinChannelInternal(sessionId: String, channelId: String) {
        sessions[sessionId]?.currentChannelId = channelId
        channelSessions.getOrPut(channelId) { mutableSetOf() }.add(sessionId)
    }

    private fun cleanupInternal(session: UserSession) {
        val sessionId = session.sessionId
        val userId = session.user.id

        session.currentChannelId?.let { leaveChannelInternal(sessionId, it) }
        userToSession.remove(userId)
        sessions.remove(sessionId)

        logger.info("Internal cleanup finished for session: $sessionId")
    }


    suspend fun getSession(sessionId: String): UserSession? = mutex.withLock {
        sessions[sessionId]
    }

    suspend fun getSessionByUserId(userId: String): UserSession? = mutex.withLock {
        userToSession[userId]?.let { sessions[it] }
    }

    suspend fun getSessionsInChannel(channelId: String): Set<String> = mutex.withLock {
        channelSessions[channelId]?.toSet() ?: emptySet()
    }

    private fun activeSessionCountUnlocked(): Int {
        val now = Clock.System.now()
        return sessions.values.count { now - it.lastHeartbeat < heartbeatTimeout }
    }

    suspend fun getActiveSessionCount(): Int = mutex.withLock {
        activeSessionCountUnlocked()
    }

    suspend fun getStatistics(): Map<String, Any> = mutex.withLock {
        return mapOf(
            "totalSessions" to sessions.size,
            "uniqueUsers" to userToSession.size,
            "activeChannels" to channelSessions.size,
            "activeSessions" to activeSessionCountUnlocked(),
            "connectionRate" to activeSessionCountUnlocked() / sessions.size.toDouble(),
            "sessionsPerChannel" to channelSessions.mapValues { it.value.size }
        )
    }

    private fun generateSessionId(session: WebSocketSession): String =
        "${session.hashCode()}_${System.currentTimeMillis()}"

    suspend fun broadcast(channelId: String, protocol: TogetherProtocol) {
        getSessionsInChannel(channelId).forEach { id ->
            val session = getSession(id)
            session?.sendSerialized(protocol)
        }
    }
}
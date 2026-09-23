package com.eimsound.eimusic.server.monitor

import com.eimsound.eimusic.server.manager.ChannelManager
import com.eimsound.eimusic.server.manager.UserSessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 连接监控器
 * 负责监控WebSocket连接状态、性能指标和异常检测
 */
class ConnectionMonitor(
    private val sessionManager: UserSessionManager,
    private val channelManager: ChannelManager
) {
    private val logger = LoggerFactory.getLogger(ConnectionMonitor::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()

    // 连接统计信息
    private var connectionStats = ConnectionStats()

    // 异常连接检测阈值
    private val abnormalDisconnectThreshold = 5 // 短时间内异常断连次数阈值
    private val abnormalTimeWindow = 1.minutes   // 时间窗口

    // 异常用户记录 user_id -> DisconnectRecord
    private val abnormalUsers = mutableMapOf<String, DisconnectRecord>()

    private val SESSION_CLEANUP_INTERVAL = 10
    private val CHANNEL_CLEANUP_INTERVAL = 30

    init {
        startMonitoring()
    }

    /**
     * 启动监控任务
     */
    private fun startMonitoring() {
        // 快速健康检查
        coroutineScope.launch {
            while (true) {
                delay(10.seconds)
                performQuickHealthCheck()
            }
        }

        // 详细统计报告
        coroutineScope.launch {
            while (true) {
                delay(1.minutes)
                generateDetailedReport()
            }
        }

        // 异常检测
        coroutineScope.launch {
            while (true) {
                delay(30.seconds)
                detectAbnormalPatterns()
            }
        }

        // 清理过期记录
        coroutineScope.launch {
            while (true) {
                delay(5.minutes)
                cleanupExpiredRecords()
            }
        }

        coroutineScope.launch {
            while (true) {
                // 定期清理空闲频道
                delay(CHANNEL_CLEANUP_INTERVAL * 1000L)
                channelManager.cleanupInactiveChannels(sessionManager)
            }
        }

        coroutineScope.launch {
            // 定期检查连接健康状态
            while (true) {
                delay(SESSION_CLEANUP_INTERVAL * 1000L)
                sessionManager.checkConnectionHealth()
            }
        }

    }

    /**
     * 快速健康检查
     */
    private suspend fun performQuickHealthCheck() = mutex.withLock {
        try {
            val stats = sessionManager.getStatistics()
            val totalSessions = stats["totalSessions"] as Int

            // 检查连接负载
            if (totalSessions > 8000) { // 假设最大连接数为10000
                logger.warn("High connection load detected: $totalSessions active sessions")
            }

            // 更新统计信息
            connectionStats.totalSessions = totalSessions
            connectionStats.activeSessions = stats["activeSessions"] as? Int ?: 0
            connectionStats.inactiveSessions = stats["inactiveSessions"] as? Int ?: 0

            // 记录峰值
            if (totalSessions > connectionStats.peakSessions) {
                connectionStats.peakSessions = totalSessions
                connectionStats.peakTime = Clock.System.now()
            }

        } catch (e: Exception) {
            logger.error("Error in quick health check: ${e.message}", e)
        }
    }

    /**
     * 生成详细报告
     */
    private suspend fun generateDetailedReport() = mutex.withLock {
        try {
            val sessionStats = sessionManager.getStatistics()
            val channelStats = channelManager.getSystemStats()

            logger.info(
                """
                === Connection Monitor Report ===
                Sessions: ${sessionStats["totalSessions"]} total, ${sessionStats["activeSessions"]} active
                Channels: ${channelStats["totalChannels"]} total, ${channelStats["onlineUsers"]} online users
                Connection Rate: ${sessionStats["connectionRate"]}
                Peak Sessions: ${connectionStats.peakSessions} at ${connectionStats.peakTime}
                ================================
                """
            )

        } catch (e: Exception) {
            logger.error("Error generating detailed report: ${e.message}", e)
        }
    }

    /**
     * 检测异常模式
     */
    private suspend fun detectAbnormalPatterns() = mutex.withLock {
        try {
            // 检测频繁断连用户
            val recentDisconnects = abnormalUsers.values.filter { record ->
                Clock.System.now() - record.lastDisconnect < abnormalTimeWindow
            }

            recentDisconnects.forEach { record ->
                if (record.disconnectCount >= abnormalDisconnectThreshold) {
                    logger.warn("Abnormal disconnection pattern detected for user ${record.userId}: ${record.disconnectCount} times in ${abnormalTimeWindow}")
                    // 可以在这里添加告警或限制措施
                }
            }

            // 检测整体异常
            val inactiveRatio = if (connectionStats.totalSessions > 0) {
                connectionStats.inactiveSessions.toDouble() / connectionStats.totalSessions
            } else 0.0

            if (inactiveRatio > 0.3) { // 超过30%连接不活跃
                logger.warn("High inactive connection ratio detected: ${(inactiveRatio * 100).toInt()}%")
            }

        } catch (e: Exception) {
            logger.error("Error in abnormal pattern detection: ${e.message}", e)
        }
    }

    /**
     * 记录用户断连事件
     */
    suspend fun recordUserDisconnect(userId: String, reason: String = "unknown") = mutex.withLock {
        try {
            val now = Clock.System.now()
            val record = abnormalUsers.getOrPut(userId) {
                DisconnectRecord(userId, mutableListOf())
            }

            record.disconnectHistory.add(DisconnectEvent(now, reason))

            // 清理过期事件
            record.disconnectHistory.removeAll { event ->
                now - event.timestamp > abnormalTimeWindow
            }

            connectionStats.totalDisconnects++
            logger.debug("Recorded disconnect for user $userId (reason: $reason)")

        } catch (e: Exception) {
            logger.error("Error recording user disconnect: ${e.message}", e)
        }
    }

    /**
     * 清理过期记录
     */
    private suspend fun cleanupExpiredRecords() = mutex.withLock {
        try {
            val now = Clock.System.now()
            val expiredUsers = abnormalUsers.filter { (_, record) ->
                record.disconnectHistory.isEmpty() ||
                        now - record.disconnectHistory.last().timestamp > abnormalTimeWindow * 2
            }.map { it.key }

            expiredUsers.forEach { userId ->
                abnormalUsers.remove(userId)
            }

            if (expiredUsers.isNotEmpty()) {
                logger.debug("Cleaned up ${expiredUsers.size} expired user records")
            }

        } catch (e: Exception) {
            logger.error("Error cleaning up expired records: ${e.message}", e)
        }
    }

    /**
     * 获取监控统计数据
     */
    suspend fun getMonitorStats(): Map<String, Any> = mutex.withLock {
        mapOf(
            "connectionStats" to connectionStats,
            "abnormalUsersCount" to abnormalUsers.size,
            "highFrequencyDisconnectors" to abnormalUsers.filter { (_, record) ->
                record.disconnectCount >= abnormalDisconnectThreshold
            }.size
        )
    }

    /**
     * 连接统计信息
     */
    data class ConnectionStats(
        var totalSessions: Int = 0,
        var activeSessions: Int = 0,
        var inactiveSessions: Int = 0,
        var peakSessions: Int = 0,
        var totalDisconnects: Long = 0,
        var peakTime: kotlin.time.Instant = Clock.System.now()
    )

    /**
     * 断连记录
     */
    data class DisconnectRecord(
        val userId: String,
        val disconnectHistory: MutableList<DisconnectEvent>
    ) {
        val disconnectCount: Int
            get() = disconnectHistory.size

        val lastDisconnect: kotlin.time.Instant
            get() = disconnectHistory.lastOrNull()?.timestamp ?: Clock.System.now()
    }

    /**
     * 断连事件
     */
    data class DisconnectEvent(
        val timestamp: kotlin.time.Instant,
        val reason: String
    )
}
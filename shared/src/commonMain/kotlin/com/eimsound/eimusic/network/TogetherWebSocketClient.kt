package com.eimsound.eimusic.network

import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.auth.AuthStore
import com.eimsound.eimusic.together.Channel
import com.eimsound.eimusic.together.ChannelEvent
import com.eimsound.eimusic.together.ChannelMessage
import com.eimsound.eimusic.together.ConnectionState
import com.eimsound.eimusic.together.PlayControlEvent
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import com.eimsound.eimusic.together.TogetherProtocol
import com.eimsound.eimusic.together.User
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 一起听WebSocket客户端
 */
class TogetherWebSocketClient(
    private val baseUrl: String = "ws://localhost:8080",
    private val client: HttpClientProvider,
    private val authStore: AuthStore
) {
    private var session: DefaultClientWebSocketSession? = null
    private val eventChannel = MutableSharedFlow<ChannelEvent>()
    private val messageChannel = MutableSharedFlow<ChannelMessage>()
    private val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 当前连接的频道
    private var currentChannelId: String? = null

    fun currentUserId(): String? = authStore.current().user.id

    // 心跳任务
    private var heartbeatJob: Job? = null
    private val heartbeatInterval = 30.seconds
    private var consecutiveHeartbeatFailures = 0
    private val maxHeartbeatFailures = 3

    // 重连相关
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelay = 3.seconds
    private val maxReconnectDelay = 30.seconds

    // 连接监控任务
    private var connectionMonitorJob: Job? = null

    init {
        observerAuthState()
    }

    private fun observerAuthState() {
        authStore.state.onEach {
            if (it is AuthState.Unauthenticated) {
                disconnect()
            }
        }.launchIn(coroutineScope)
    }


    /**
     * 获取当前连接状态
     */
    val connectionStateFlow: StateFlow<ConnectionState> = connectionState.asStateFlow()

    /**
     * 监听频道事件流
     */
    fun channelEvents(): Flow<ChannelEvent> = eventChannel

    /**
     * 监听消息流
     */
    fun messages(): Flow<ChannelMessage> = messageChannel

    /**
     * 连接到WebSocket服务器
     */
    suspend fun connect(): Result<Unit> {
        return connectWithRetry(maxRetries = 1)
    }

    /**
     * 带重试的连接方法
     */
    private suspend fun connectWithRetry(maxRetries: Int = 1): Result<Unit> = try {
        connectionState.value = ConnectionState.CONNECTING

        session = client.client.webSocketSession("$baseUrl/together")
        startHeartbeat()
        listen()
        startConnectionMonitor()

        connectionState.value = ConnectionState.CONNECTED
        reconnectAttempts = 0 // 重置重连计数

        Result.success(Unit)
    } catch (e: Exception) {
        connectionState.value = ConnectionState.DISCONNECTED
        if (maxRetries > 0) {
            // 简单重试机制
            delay(reconnectDelay)
            connectWithRetry(maxRetries - 1)
        } else {
            Result.failure(e)
        }
    }

    /**
     * 断开连接
     */
    suspend fun disconnect() {
        connectionState.value = ConnectionState.DISCONNECTED
        stopReconnection()
        heartbeatJob?.cancel()
        connectionMonitorJob?.cancel()
        session?.close()
        session = null
    }

    /**
     * 加入频道
     */
    suspend fun joinChannel(channelId: String, syncState: SyncState): Result<Channel> = try {
        val user = authStore.current().user

        val message = TogetherProtocol.JoinChannel(
            channelId = channelId,
            syncState = syncState,
            timestamp = Clock.System.now()
        )

        // 先订阅再发送，避免服务器响应过快导致 SharedFlow 事件丢失。
        return withTimeout(5.seconds) {
            coroutineScope {
                val response = async {
                    channelEvents().first {
                        it is ChannelEvent.RoomMetadataChanged && it.channel.members.any { member -> user.id == member.id }
                    } as ChannelEvent.RoomMetadataChanged
                }
                sendMessage(message)
                currentChannelId = channelId
                Result.success(response.await().channel)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 离开频道
     */
    suspend fun leaveChannel(): Result<Unit> = try {
        val message = TogetherProtocol.LeaveChannel(
            channelId = currentChannelId ?: return Result.failure(IllegalStateException("未加入任何频道")),
            timestamp = Clock.System.now()
        )

        sendMessage(message)
        currentChannelId = null
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 发送文本消息
     */
    suspend fun sendChatMessage(content: String): Result<Unit> = try {
        val currentChannel = currentChannelId ?: return Result.failure(IllegalStateException("未加入任何频道"))

        val message = TogetherProtocol.SendChat(
            channelId = currentChannel,
            content = content,
            timestamp = Clock.System.now()
        )

        sendMessage(message)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 发送播放控制命令
     */
    suspend fun sendPlayControl(
        event: PlayControlEvent,
        commandId: String? = null,
    ): Result<Unit> = try {
        val currentChannel = currentChannelId ?: return Result.failure(IllegalStateException("未加入任何频道"))

        val message = TogetherProtocol.PlayControlAction(
            channelId = currentChannel,
            event = event,
            commandId = commandId,
            timestamp = Clock.System.now()
        )

        sendMessage(message)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 请求同步状态
     */
    suspend fun requestSync(): Result<Unit> = try {
        val currentChannel = currentChannelId ?: return Result.failure(IllegalStateException("未加入任何频道"))

        val message = TogetherProtocol.Heartbeat(
            timestamp = Clock.System.now()
        )

        sendMessage(message)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }


    /**
     * 发送WebSocket消息
     */
    private suspend fun sendMessage(message: TogetherProtocol) {
        val session = session ?: throw IllegalStateException("WebSocket未连接")
        session.sendSerialized(message)
    }

    /**
     * 开始心跳检测
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        consecutiveHeartbeatFailures = 0
        heartbeatJob = coroutineScope.launch {
            while (isActive) {
                delay(heartbeatInterval)
                try {
                    val heartbeatMessage = TogetherProtocol.Heartbeat(
                        timestamp = Clock.System.now()
                    )
                    sendMessage(heartbeatMessage)
                    consecutiveHeartbeatFailures = 0
                } catch (e: Exception) {
                    consecutiveHeartbeatFailures++
                    if (consecutiveHeartbeatFailures >= maxHeartbeatFailures) {
                        handleConnectionLost()
                        break
                    }
                }
            }
        }
    }

    /**
     * 监听服务器消息
     */
    private fun listen() {
        coroutineScope.launch {
            try {
                session?.run {
                    while (true) {
                        val data = receiveDeserialized<TogetherProtocol>()
                        handleIncomingMessage(data)
                    }
                }
            } catch (e: Exception) {
                // 连接异常处理
                handleConnectionLost()
            }finally {
                disconnect()
            }
        }
    }

    /**
     * 处理接收到的消息
     */
    private suspend fun handleIncomingMessage(message: TogetherProtocol) {
        try {
            when (message) {
                is TogetherProtocol.ServerEvent -> serverEvent(message)
                is TogetherProtocol.MessageReceived -> messageReceived(message)
                is TogetherProtocol.ErrorResponse -> println("Error: ${message.message}")
                else -> {
                    // 处理其他类型的消息
                }
            }
        } catch (e: Exception) {
            // 解析错误处理
            e.printStackTrace()
        }
    }

    private suspend fun serverEvent(event: TogetherProtocol.ServerEvent) {
        eventChannel.emit(event.event)
    }

    private suspend fun messageReceived(message: TogetherProtocol.MessageReceived) {
        messageChannel.emit(message.message)
    }

    /**
     * 处理连接丢失
     */
    private fun handleConnectionLost() {
        if (connectionState.value != ConnectionState.RECONNECTING) {
            connectionState.value = ConnectionState.DISCONNECTED
            session = null
            heartbeatJob?.cancel()
            startReconnection()
        }
    }

    /**
     * 开始重连机制
     */
    private fun startReconnection() {
        if (reconnectJob?.isActive == true) return

        connectionState.value = ConnectionState.RECONNECTING
        reconnectJob = coroutineScope.launch {
            while (reconnectAttempts < maxReconnectAttempts && isActive) {
                reconnectAttempts++
                val delayTime = calculateReconnectDelay(reconnectAttempts)

                println("第${reconnectAttempts}次重连尝试，等待${delayTime.inWholeSeconds}秒...")
                delay(delayTime)

                try {
                    val result = connectWithRetry(maxRetries = 0) // 不再递归重试
                    if (result.isSuccess) {
                        println("重连成功!")
                        // 如果之前在频道中，重新加入
//                        currentChannelId?.let { channelId ->
//                            coroutineScope.launch {
//                                joinChannel(channelId)
//                            }
//                        }
                        break
                    }
                } catch (e: Exception) {
                    println("重连失败：${e.message}")
                }
            }

            if (reconnectAttempts >= maxReconnectAttempts) {
                connectionState.value = ConnectionState.DISCONNECTED
                println("达到最大重连次数，停止重连")
            }
        }
    }

    /**
     * 计算重连延迟时间（指数退避算法）
     */
    private fun calculateReconnectDelay(attempt: Int): Duration {
        val baseDelay = reconnectDelay.inWholeMilliseconds
        val exponentialDelay = baseDelay * (1L shl (attempt - 1)) // 2^(attempt-1)
        val jitter = (Random.nextInt() * 1000).toLong() // 添加随机抖动
        val delay = (exponentialDelay + jitter).coerceAtMost(maxReconnectDelay.inWholeMilliseconds)
        return delay.milliseconds
    }

    /**
     * 停止重连
     */
    private fun stopReconnection() {
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempts = 0
    }

    /**
     * 开始连接监控
     */
    private fun startConnectionMonitor() {
        connectionMonitorJob?.cancel()
        connectionMonitorJob = coroutineScope.launch {
            while (isActive) {
                delay(5.seconds) // 每5秒检查一次连接状态
                if (connectionState.value == ConnectionState.CONNECTED) {
                    // 检查session是否仍然有效
                    if (session?.isActive != true) {
                        println("检测到连接失效，启动重连")
                        handleConnectionLost()
                    }
                }
            }
        }
    }
}
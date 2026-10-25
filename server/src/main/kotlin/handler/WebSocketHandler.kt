package com.eimsound.eimusic.server.handler

import com.eimsound.eimusic.cycle
import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.server.manager.ChannelManager
import com.eimsound.eimusic.together.CreateChannelResult
import com.eimsound.eimusic.server.manager.CreateUserSessionResult
import com.eimsound.eimusic.together.JoinChannelResult
import com.eimsound.eimusic.together.LeaveChannelResult
import com.eimsound.eimusic.server.manager.UserSessionManager
import com.eimsound.eimusic.server.model.SYSTEM
import com.eimsound.eimusic.server.model.UserSession
import com.eimsound.eimusic.server.monitor.ConnectionMonitor
import com.eimsound.eimusic.together.Channel
import com.eimsound.eimusic.together.ChannelEvent
import com.eimsound.eimusic.together.ChannelMessage
import com.eimsound.eimusic.together.PlayControlEvent
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import com.eimsound.eimusic.together.SystemMessageType
import com.eimsound.eimusic.together.TogetherProtocol
import com.eimsound.eimusic.together.User
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import org.slf4j.LoggerFactory
import kotlin.time.Clock

/**
 * WebSocket消息处理器
 * 处理所有WebSocket通信逻辑
 */
class WebSocketHandler(
    private val channelManager: ChannelManager,
    private val sessionManager: UserSessionManager,
    private val connectionMonitor: ConnectionMonitor? = null,
) {
    private val logger = LoggerFactory.getLogger(WebSocketHandler::class.java)

    /**
     * 处理WebSocket会话
     */
    suspend fun handleSession(session: DefaultWebSocketServerSession, user: User) {
        // 同一用户重连时先踢掉旧连接，避免旧 listen 协程继续占用会话。
        cleanupPreviousSession(user)

        when (val result = sessionManager.createSession(session, user, forceCleanup = false)) {
            is CreateUserSessionResult.Success -> {
                listen(result.session)
            }

            is CreateUserSessionResult.Error -> {
                sendToUser(session, TogetherProtocol.ErrorResponse(result.message))
            }

            CreateUserSessionResult.LimitExceeded -> {
                sendToUser(session, TogetherProtocol.ErrorResponse("当前会话数过多，请稍后再试"))
            }

            CreateUserSessionResult.Exists -> {
                sendToUser(session, TogetherProtocol.ErrorResponse("当前用户已登录"))
            }
        }
    }

    private suspend fun cleanupPreviousSession(user: User) {
        val previousSession = sessionManager.getSessionByUserId(user.id) ?: return

        logger.warn(
            "Found existing session {} for user {}, cleaning it up before accepting new connection",
            previousSession.sessionId,
            user.username
        )

        try {
            handleLeaveChannel(previousSession)
        } catch (e: Exception) {
            logger.warn(
                "Failed to leave channel while cleaning previous session {} for user {}: {}",
                previousSession.sessionId,
                user.username,
                e.message
            )
        }

        sessionManager.removeSession(previousSession.sessionId)

        try {
            previousSession.session.close(
                CloseReason(CloseReason.Codes.NORMAL, "Replaced by a new login")
            )
        } catch (e: Exception) {
            logger.warn(
                "Failed to close previous WebSocket session {} for user {}: {}",
                previousSession.sessionId,
                user.username,
                e.message
            )
        }
    }

    private suspend fun listen(userSession: UserSession) {
        val user = userSession.user
        val session = userSession.session
        try {
            logger.info("Starting WebSocket session handling for user ${user.username} (${userSession.sessionId})")
            while (true) {
                val data = session.receiveDeserialized<TogetherProtocol>()
                handleAction(userSession, data)
            }
        } catch (e: Exception) {
            logger.error("Session error for user ${user.username} (${userSession.sessionId}): ${e.message}", e)
            // 记录异常断连
            connectionMonitor?.recordUserDisconnect(user.id, e.message ?: "exception")
        } finally {
            logger.info("Cleaning up session for user ${user.username} (${userSession.sessionId}), current channel: ${userSession.currentChannelId}")
            sessionManager.removeSession(userSession.sessionId)
            logger.info("Session cleanup completed for user ${user.username} (${userSession.sessionId})")
        }
    }

    private suspend fun handleAction(session: UserSession, action: TogetherProtocol) {
        when (action) {
            is TogetherProtocol.JoinChannel -> handleJoinChannel(session, action)
            is TogetherProtocol.LeaveChannel -> handleLeaveChannel(session)
            is TogetherProtocol.PlayControlAction -> handleControl(session, action)
            is TogetherProtocol.SendChat -> handleSendMessage(session, action)
            is TogetherProtocol.Heartbeat -> handleHeartbeat(session)
            else -> {
                logger.warn("Client sent a Server-only response type: ${action::class.simpleName}")
                sendToUser(session, TogetherProtocol.ErrorResponse("非法操作"))
            }
        }
    }

    private suspend fun handleControl(session: UserSession, action: TogetherProtocol.PlayControlAction) {
        if (!ensureMember(session, action.channelId)) return
        val event = action.event
        val localTrack: Track? = when (event) {
            is PlayControlEvent.ChangeTrack -> event.track
            is PlayControlEvent.AddToQueue -> event.track
            is PlayControlEvent.Next -> event.track
            is PlayControlEvent.Previous -> event.track
            is PlayControlEvent.ChangeTrackList ->
                event.trackList.firstOrNull { it.source == TrackSource.LOCAL }
                    ?: event.play?.takeIf { it.source == TrackSource.LOCAL }
            else -> null
        }
        if (localTrack != null) {
            sendErrorResponse(session, "本地曲目无法一起听")
            return
        }
        var isShowMessage = true
        val state = when (event) {
            is PlayControlEvent.Play -> {
                channelManager.updatePlayState(action.channelId) {
                    SyncState(
                        playState = PlayState.PLAYING,
                        position = event.position,
                        anchorTimestamp = Clock.System.now(),
                        controlledBy = session.user
                    )
                }
            }

            is PlayControlEvent.Pause -> {
                channelManager.updatePlayState(action.channelId) {
                    SyncState(
                        playState = PlayState.PAUSED,
                        position = event.position,
                        anchorTimestamp = Clock.System.now(),
                        controlledBy = session.user
                    )
                }
            }

            is PlayControlEvent.Seek -> {
                channelManager.updatePlayState(action.channelId) {
                    SyncState(
                        position = event.position,
                        anchorTimestamp = Clock.System.now(),
                        controlledBy = session.user
                    )
                }
            }

            is PlayControlEvent.AddToQueue -> {
                channelManager.updatePlayState(action.channelId) {
                    val index = when (val strategy = event.strategy) {
                        is AddTrackStrategy.AddToLast -> {
                            it.trackList.size
                        }

                        is AddTrackStrategy.AddToNext -> {
                            it.trackList.indexOfFirst { track -> track.id == it.currentTrack?.id } + 1
                        }

                        is AddTrackStrategy.InsertToQueue -> {
                            strategy.index
                        }
                    }

                    val existingIndex = it.trackList.indexOfFirst { track -> track.id == event.track.id }
                    val updatedQueue = it.trackList.toMutableList().apply {
                        if (existingIndex != -1) {
                            removeAt(existingIndex)
                        }
                        add(index, event.track)
                    }
                    SyncState(
                        trackList = updatedQueue,
                        controlledBy = session.user
                    )
                }
            }

            is PlayControlEvent.RemoveFromQueue -> {
                channelManager.updatePlayState(action.channelId) {
                    SyncState(
                        trackList = it.trackList.toMutableList().apply {
                            removeAll { track -> track.id == event.track.id }
                        },
                        controlledBy = session.user
                    )
                }
            }

            is PlayControlEvent.ChangeTrack -> {
                channelManager.updatePlayState(action.channelId) {
                    if (!event.isAudioCompleted) {
                        SyncState(
                            track = event.track,
                            position = 0L,
                            anchorTimestamp = Clock.System.now(),
                            controlledBy = session.user
                        )
                    } else {
                        isShowMessage = false
                        if (session.user.id == it.owner.id) {
                            SyncState(
                                track = event.track,
                                position = 0L,
                                anchorTimestamp = Clock.System.now(),
                                controlledBy = session.user
                            )
                        } else null
                    }
                }
            }

            is PlayControlEvent.ChangeTrackList -> {
                if (event.trackList.isEmpty()) {
                    channelManager.clearQueue(action.channelId)
                } else {
                    channelManager.updatePlayState(action.channelId) {
                        SyncState(
                            track = event.play ?: event.trackList.first(),
                            trackList = event.trackList,
                            controlledBy = session.user
                        )
                    }
                }
            }

            is PlayControlEvent.PlayModeChanged -> {
                channelManager.updatePlayState(action.channelId) {
                    SyncState(playMode = event.mode, controlledBy = session.user)
                }
            }

            is PlayControlEvent.Next -> {
                channelManager.updatePlayState(action.channelId) {
                    if (it.trackList.isNotEmpty()) {
                        SyncState(
                            track = event.track,
                            position = 0L,
                            anchorTimestamp = Clock.System.now(),
                            controlledBy = session.user
                        )
                    } else {
                        null
                    }
                }
            }

            is PlayControlEvent.Previous -> {
                channelManager.updatePlayState(action.channelId) {
                    if (it.trackList.isNotEmpty()) {
                        SyncState(
                            track = event.track,
                            position = 0L,
                            anchorTimestamp = Clock.System.now(),
                            controlledBy = session.user
                        )
                    } else {
                        null
                    }
                }
            }

            is PlayControlEvent.Stop -> {
                channelManager.updatePlayState(action.channelId) {
                    SyncState(
                        track = null,
                        position = 0L,
                        anchorTimestamp = Clock.System.now(),
                        controlledBy = session.user
                    )
                }
            }
        }

        if (state != null) {
            // 不回发给操作者，避免客户端把刚发送的队列/播放状态再应用一次。
            broadcast(
                action.channelId,
                TogetherProtocol.ServerEvent(ChannelEvent.PlayStateChanged(action.channelId, event, state)),
                exceptSessionId = session.sessionId,
            )

            if (isShowMessage) {
                broadcast(
                    action.channelId, TogetherProtocol.MessageReceived(
                        ChannelMessage.PlayControl(
                            channelId = action.channelId,
                            sender = session.user,
                            event = event,
                        )
                    )
                )
            }
        }
    }

    private suspend fun broadcast(
        channelId: String,
        protocol: TogetherProtocol,
        exceptSessionId: String? = null,
    ) {
        sessionManager.broadcast(channelId, protocol, exceptSessionId)
    }

    private suspend fun sendToUser(session: DefaultWebSocketServerSession, msg: TogetherProtocol) {
        session.sendSerialized(msg)
    }

    /**
     * 处理加入频道请求
     * 优化后的版本：减少嵌套层级，提高可读性
     */
    private suspend fun handleJoinChannel(
        session: UserSession,
        action: TogetherProtocol.JoinChannel,
    ) {
        val channelId = action.channelId
        val user = session.user
        // 如果频道不存在，先尝试创建
        if (channelManager.getChannel(channelId) == null) {
            handleCreateChannel(session, channelId, action.syncState)
            return
        }

        // 频道已存在，处理加入逻辑
        handleExistingChannelJoin(session, channelId, user)
    }

    /**
     * 处理创建新频道的逻辑
     */
    private suspend fun handleCreateChannel(session: UserSession, channelId: String, syncState: SyncState) {
        when (val result = channelManager.createChannel(channelId, channelId, session, syncState)) {
            is CreateChannelResult.Exists -> {
                // 频道意外地已经存在，转为加入逻辑
                handleExistingChannelJoin(session, channelId, session.user)
            }

            is CreateChannelResult.LimitExceeded -> {
                sendErrorResponse(session, "频道数量已满")
            }

            is CreateChannelResult.Error -> {
                sendErrorResponse(session, result.message)
            }

            is CreateChannelResult.Success -> {
                val channel = result.channel
                completeJoinProcess(session, channel, session.user)
            }
        }
    }

    /**
     * 处理加入已有频道的逻辑
     */
    private suspend fun handleExistingChannelJoin(session: UserSession, channelId: String, user: User) {
        when (val result = channelManager.joinChannel(channelId, session)) {
            is JoinChannelResult.AlreadyInChannel -> {
                completeJoinProcess(session, result.channel, user)
            }

            JoinChannelResult.ChannelFull -> {
                sendErrorResponse(session, "频道已满")
            }

            is JoinChannelResult.Error -> {
                sendErrorResponse(session, result.message)
            }

            JoinChannelResult.NotFound -> {
                sendErrorResponse(session, "频道不存在")
            }

            is JoinChannelResult.Success -> {
                completeJoinProcess(session, result.channel, user)
            }
        }
    }

    /**
     * 完成加入频道
     */
    private suspend fun completeJoinProcess(session: UserSession, channel: Channel, user: User) {
        val channelId = channel.id

        // 1. 更新会话状态
        sessionManager.joinChannel(session.sessionId, channelId)

        // 2. 发送初始快照给新用户
        sendRoomMetadata(session, channelId, channel)

        // 3. 广播用户加入事件
        broadcastUserJoinEvents(channelId, user, channel.members)
    }

    /**
     * 统一的错误响应发送方法
     */
    private suspend fun sendErrorResponse(session: UserSession, message: String) {
        sendToUser(session, TogetherProtocol.ErrorResponse(message))
    }

    /**
     * 发送房间元数据
     */
    private suspend fun sendRoomMetadata(session: UserSession, channelId: String, channel: Channel) {
        sendToUser(
            session,
            TogetherProtocol.ServerEvent(
                ChannelEvent.RoomMetadataChanged(channelId, channel)
            )
        )
    }

    /**
     * 广播用户加入相关事件
     */
    private suspend fun broadcastUserJoinEvents(channelId: String, user: User, members: List<User>) {
        // 广播用户加入事件
        broadcast(channelId, TogetherProtocol.ServerEvent(ChannelEvent.UserJoined(channelId, user, members)))

        // 广播系统消息
        broadcast(
            channelId,
            TogetherProtocol.MessageReceived(
                ChannelMessage.System(
                    channelId,
                    SYSTEM,
                    content = "${user.username} 加入了房间",
                    messageType = SystemMessageType.JOIN
                )
            )
        )
    }

    /**
     * 处理离开频道请求
     */
    private suspend fun handleLeaveChannel(
        session: UserSession,
    ) {
        val channelId = session.currentChannelId
        val user = session.user
        when (val result = channelManager.leaveChannel(channelId, session)) {
            is LeaveChannelResult.Error -> {
                logger.error("Error for user ${user.username} (${session.sessionId}): ${result.message}")
                sendErrorResponse(session, result.message)
            }

            is LeaveChannelResult.NotFound -> {
                sendErrorResponse(session, "频道不存在")
            }

            is LeaveChannelResult.NotInChannel -> {
                sendErrorResponse(session, "用户未加入该频道")
            }

            is LeaveChannelResult.Success -> {
                logger.debug("Removing user ${user.id} from channel $channelId")
                val channel = result.channel
                sessionManager.leaveChannel(session.sessionId, channel.id)

                handleOwnerChange(channel.id, user.id)

                broadcast(
                    channel.id,
                    TogetherProtocol.ServerEvent(ChannelEvent.UserLeft(channel.id, user, channel.members))
                )

                broadcast(
                    channel.id, TogetherProtocol.MessageReceived(
                        ChannelMessage.System(
                            channel.id,
                            SYSTEM,
                            content = "${user.username} 离开了",
                            messageType = SystemMessageType.LEAVE
                        )
                    )
                )
            }

            is LeaveChannelResult.SuccessAndChannelEmpty -> {
                logger.info("Channel $channelId is now empty, deleting channel")
                channelManager.deleteChannel(result.channel.id)
            }
        }
    }

    /**
     * 处理发送消息请求
     */
    private suspend fun handleSendMessage(
        session: UserSession,
        action: TogetherProtocol.SendChat,
    ) {
        if (!ensureMember(session, action.channelId)) return
        val chat = ChannelMessage.Text(action.channelId, session.user, content = action.content)
        broadcast(action.channelId, TogetherProtocol.MessageReceived(chat))
    }


    /**
     * 处理心跳
     */
    /** 校验发送者确实在当前频道，避免伪造 channelId 控制别人的房间。 */
    private suspend fun ensureMember(session: UserSession, channelId: String): Boolean {
        if (session.currentChannelId != channelId || channelManager.getChannel(channelId) == null) {
            sendErrorResponse(session, "未加入该频道")
            return false
        }
        return true
    }

    private suspend fun handleHeartbeat(
        session: UserSession,
    ) {
        sessionManager.updateHeartbeat(session.sessionId)
    }


    /**
     * 发送错误消息
     */
//    private suspend fun sendErrorMessage(session: UserSession, errorMessage: String) {
//        val errorResponse = WebSocketMessage(
//            type = MessageType.ERROR,
//            payload = format.encodeToString(mapOf("error" to errorMessage)),
//            timestamp = Clock.System.now()
//        )
//
//        session.send(Frame.Text(format.encodeToString(errorResponse)))
//    }


    /**
     * (进阶) 处理房主转让
     */
    private suspend fun handleOwnerChange(channelId: String, oldOwnerId: String) {
        val newOwner = channelManager.promoteNextOwner(channelId, oldOwnerId) ?: return
        broadcast(
            channelId,
            TogetherProtocol.MessageReceived(
                ChannelMessage.System(
                    channelId,
                    SYSTEM,
                    content = newOwner.user.username + " 自动成为新房主",
                    messageType = SystemMessageType.JOIN,
                ),
            ),
        )
    }
}

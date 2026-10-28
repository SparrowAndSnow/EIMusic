package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.TogetherFocus
import com.eimsound.eimusic.keyboard.TogetherKeyActions
import com.eimsound.eimusic.keyboard.dispatchTogetherKey
import com.eimsound.eimusic.keyboard.togetherStatusBarHint
import com.eimsound.eimusic.together.ChannelMessage
import com.eimsound.eimusic.together.ChannelState
import com.eimsound.eimusic.together.ConnectionState
import com.eimsound.eimusic.ui.components.InputField
import com.eimsound.eimusic.ui.components.ListView
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.render
import com.eimsound.eimusic.viewmodel.ChannelViewModel
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.*
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.*

@Composable
fun TogetherScreen(
    channelViewModel: ChannelViewModel,
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val safeArea = LocalSafeContentSize.current
    val state = channelViewModel.uiState.collectAsState().value
    val termWidth = safeArea.columns
    val termHeight = safeArea.rows
    var focus by remember { mutableStateOf(TogetherFocus.Welcome) }

    LaunchedEffect(Unit) {
        if (state.connectionState == ConnectionState.DISCONNECTED) {
            channelViewModel.connect()
        }
    }

    LaunchedEffect(state.channelState) {
        if (state.channelState is ChannelState.Joined && focus == TogetherFocus.Welcome) {
            focus = TogetherFocus.Messages
        }
    }

    val isJoined = state.channelState is ChannelState.Joined

    ScreenFrame(
        hint = togetherStatusBarHint(focus),
        onKey = { event ->
            dispatchTogetherKey(
                event = event,
                focus = focus,
                isJoined = isJoined,
                actions = TogetherKeyActions(
                    joinChannel = { channelViewModel.joinChannel("default") },
                    leaveChannel = { channelViewModel.leaveChannel() },
                    setFocus = { newFocus ->
                        focus = newFocus
                        channelViewModel.setChatInput(newFocus == TogetherFocus.ChatInput)
                    },
                    goBack = {
                        channelViewModel.setChatInput(false)
                        channelViewModel.disconnect()
                        onGoBack()
                    },
                ),
            )
        },
    ) {
        Column {
            when (focus) {
                TogetherFocus.Welcome -> WelcomeView(
                    connectionState = state.connectionState,
                    palette = palette,
                )
                TogetherFocus.ChannelIdInput -> ChannelIdInputView(
                    value = state.channelIdInput,
                    onValueChange = { channelViewModel.updateChannelIdInput(it) },
                    onConfirm = {
                        channelViewModel.joinChannel(state.channelIdInput.ifBlank { "default" })
                        focus = TogetherFocus.Messages
                    },
                    onCancel = { focus = TogetherFocus.Welcome },
                )
                TogetherFocus.Messages, TogetherFocus.ChatInput -> ChatLayout(
                    focus = focus,
                    state = state,
                    termHeight = termHeight,
                    termWidth = termWidth,
                    channelViewModel = channelViewModel,
                    palette = palette,
                    onSendMessage = {
                        if (state.chatInput.isNotBlank()) {
                            channelViewModel.sendChatMessage(state.chatInput)
                            channelViewModel.updateChatInput("")
                        }
                        // 保持输入态继续聊天；清空内容即可，避免隐藏输入框后留下残影。
                    },
                    onCancelChat = {
                        channelViewModel.setChatInput(false)
                        focus = TogetherFocus.Messages
                    },
                )
            }
        }
    }
}

@Composable
private fun WelcomeView(
    connectionState: ConnectionState,
    palette: com.eimsound.eimusic.ui.theme.TuiColors,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text("  👥", color = palette.textFg)
            Spacer(Modifier.height(1))

            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = palette.titleFg)) { append("  T O G E T H E R") }
            })
            Spacer(Modifier.height(1))

            Text("  Listen together in real-time", color = palette.textDimFg)
            Spacer(Modifier.height(2))

            val (dot, label, dotColor) = when (connectionState) {
                ConnectionState.CONNECTED -> Triple("●", "Connected", palette.highlightFg)
                ConnectionState.CONNECTING -> Triple("◌", "Connecting...", palette.textDimFg)
                ConnectionState.RECONNECTING -> Triple("◌", "Reconnecting...", palette.textDimFg)
                ConnectionState.DISCONNECTED -> Triple("○", "Disconnected", palette.textDimFg)
            }
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = dotColor)) { append("  $dot") }
                withStyle(SpanStyle(color = palette.textDimFg)) { append("  $label") }
            })
            Spacer(Modifier.height(3))

            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = palette.highlightFg)) { append("  j") }
                withStyle(SpanStyle(color = palette.textDimFg)) { append("  Join default channel") }
            })
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = palette.highlightFg)) { append("  l") }
                withStyle(SpanStyle(color = palette.textDimFg)) { append("  Leave / Disconnect") }
            })
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = palette.highlightFg)) { append("  Esc") }
                withStyle(SpanStyle(color = palette.textDimFg)) { append("  Back") }
            })
        }
    }
}

@Composable
private fun ChannelIdInputView(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val palette = LocalTuiColors.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text(" Enter channel ID:", color = palette.titleFg)
            Spacer(Modifier.height(1))
            Box(modifier = Modifier.border(color = palette.dimBorderFg)) {
                InputField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = "default",
                    onConfirm = onConfirm,
                    onCancel = onCancel,
                )
            }
            Spacer(Modifier.height(1))
            Text(" Enter to join  ·  Esc to cancel", color = palette.textDimFg)
        }
    }
}

@Composable
private fun ChatLayout(
    focus: TogetherFocus,
    state: com.eimsound.eimusic.viewmodel.TogetherUiState,

    termHeight: Int,
    termWidth: Int,
    channelViewModel: ChannelViewModel,
    palette: com.eimsound.eimusic.ui.theme.TuiColors,
    onSendMessage: () -> Unit = {},
    onCancelChat: () -> Unit = {},
) {
    ConnectionBar(state.connectionState, palette)

    val msgLines = (termHeight - 2 - if (state.isChatInput) 2 else 0).coerceAtLeast(1)
    MessageListView(
        messages = state.messages,
        visibleLines = msgLines,
        termWidth = termWidth,
        channelViewModel = channelViewModel,
        palette = palette,
        enabled = focus == TogetherFocus.Messages,
    )

    if (state.isChatInput) {
        Spacer(Modifier.height(1))
        Box(modifier = Modifier.border(color = palette.dimBorderFg)) {
            InputField(
                value = state.chatInput,
                onValueChange = { channelViewModel.updateChatInput(it) },
                placeholder = "Type a message and press Enter...",
                onConfirm = onSendMessage,
                onCancel = onCancelChat,
            )
        }
    }
}

@Composable
private fun ConnectionBar(
    connectionState: ConnectionState,
    palette: com.eimsound.eimusic.ui.theme.TuiColors,
) {
    val (dot, label, dotColor) = when (connectionState) {
        ConnectionState.CONNECTED -> Triple("●", "Connected", palette.highlightFg)
        ConnectionState.CONNECTING -> Triple("◌", "Connecting...", palette.textDimFg)
        ConnectionState.RECONNECTING -> Triple("◌", "Reconnecting...", palette.textDimFg)
        ConnectionState.DISCONNECTED -> Triple("○", "Disconnected", palette.textDimFg)
    }
    Text(buildAnnotatedString {
        withStyle(SpanStyle(color = dotColor)) { append("  $dot") }
        withStyle(SpanStyle(color = palette.textDimFg)) { append("  $label") }
    })
}

@Composable
private fun MessageListView(
    messages: List<ChannelMessage>,
    visibleLines: Int,
    termWidth: Int,
    channelViewModel: ChannelViewModel,
    palette: com.eimsound.eimusic.ui.theme.TuiColors,
    enabled: Boolean = true,
) {
    if (messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                Text("  No messages yet.", color = palette.textDimFg)
                Text("  Press 'm' to start chatting.", color = palette.textDimFg)
            }
        }
        return
    }

    ListView(
        items = messages,
        visibleLines = visibleLines,
        enabled = enabled,
        showScrollIndicator = true,
        render = { message, i, isSelected ->
            ChatBubble(
                message = message,
                isSelected = isSelected,
                isCurrentUser = channelViewModel.isMessageCurrentUser(message),
                termWidth = termWidth,
                palette = palette,
                channelViewModel = channelViewModel,
            )
        },
    )
}

@Composable
private fun ChatBubble(
    message: ChannelMessage,
    isSelected: Boolean,
    isCurrentUser: Boolean,
    termWidth: Int,
    palette: com.eimsound.eimusic.ui.theme.TuiColors,
    channelViewModel: ChannelViewModel,
) {
    val maxContentWidth = (termWidth * 0.55).toInt().coerceIn(20, 50)

    when (message) {
        is ChannelMessage.Text -> {
            val senderName = message.sender.username.render()
            val content = message.content.render()
            val timestamp = channelViewModel.formatTimestamp(message.timestamp)
            val avatar = message.sender.username.firstOrNull()?.uppercase() ?: "?"
            val align = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
            val borderColor = if (isCurrentUser) palette.highlightFg else palette.borderFg

            val selectionPrefix = if (isSelected) "▸" else " "

            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .align(align)
                        .border(color = borderColor)
                ) {
                    Column {
                        Row {
                            Text(selectionPrefix, color = if (isSelected) palette.currentMarkerFg else palette.textDimFg)
                            Text("($avatar) ", color = palette.titleFg)
                            Text(
                                senderName,
                                Modifier.widthIn(max = 15),
                                color = if (isCurrentUser) palette.highlightFg else palette.textFg,
                                softWrap = true,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("  ", color = palette.textDimFg)
                            Text(timestamp, color = palette.textDimFg)
                        }
                        Text(
                            buildAnnotatedString {
                                append("  ")
                                withStyle(SpanStyle(color = palette.textFg)) { append(content) }
                            },
                            Modifier.widthIn(max = maxContentWidth),
                            softWrap = true,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        is ChannelMessage.System -> {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    buildAnnotatedString {
                        val sel = if (isSelected) "▸ " else ""
                        append("  $sel* ${message.content.render()}")
                    },
                    color = palette.textDimFg,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    textAlign = Alignment.CenterHorizontally,
                    softWrap = true,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        is ChannelMessage.PlayControl -> {
            val sender = message.sender.username.render()
            val ev = message.event
            val action = when (ev) {
                is com.eimsound.eimusic.together.PlayControlEvent.Play -> "▶ played"
                is com.eimsound.eimusic.together.PlayControlEvent.Pause -> "⏸ paused"
                is com.eimsound.eimusic.together.PlayControlEvent.ChangeTrack -> "🎵 changed to ${ev.track.name}"
                is com.eimsound.eimusic.together.PlayControlEvent.Seek -> "⏩ seeked"
                is com.eimsound.eimusic.together.PlayControlEvent.Next -> "⏭ skipped"
                is com.eimsound.eimusic.together.PlayControlEvent.Previous -> "⏮ went back"
                is com.eimsound.eimusic.together.PlayControlEvent.PlayModeChanged -> "🔄 switched mode"
                else -> "⚡ controlled"
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    buildAnnotatedString {
                        val sel = if (isSelected) "▸ " else ""
                        append("  $sel$sender $action")
                    },
                    color = palette.textDimFg,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    textAlign = Alignment.CenterHorizontally,
                    softWrap = true,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

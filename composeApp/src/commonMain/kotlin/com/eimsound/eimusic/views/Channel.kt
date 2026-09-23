package com.eimsound.eimusic.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.eimsound.eimusic.components.ColumnList
import com.eimsound.eimusic.components.ConnectionStatusIndicator
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.together.Channel
import com.eimsound.eimusic.together.ChannelMessage
import com.eimsound.eimusic.together.ChannelState
import com.eimsound.eimusic.together.ConnectionState
import com.eimsound.eimusic.together.DEFAULT_CHANNEL_ID
import com.eimsound.eimusic.together.User
import com.eimsound.eimusic.viewmodel.ChannelViewModel
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.arrow_back_24px
import eimusic.composeapp.generated.resources.cancel
import eimusic.composeapp.generated.resources.close
import eimusic.composeapp.generated.resources.group_24px
import eimusic.composeapp.generated.resources.join
import eimusic.composeapp.generated.resources.send_24px
import eimusic.composeapp.generated.resources.together_channel_id
import eimusic.composeapp.generated.resources.together_create_channel
import eimusic.composeapp.generated.resources.together_description
import eimusic.composeapp.generated.resources.together_join_channel
import eimusic.composeapp.generated.resources.together_join_default_channel
import eimusic.composeapp.generated.resources.together_members
import eimusic.composeapp.generated.resources.together_message_hint
import eimusic.composeapp.generated.resources.together_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

@Composable
fun ChannelView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val channelViewModel = koinViewModel<ChannelViewModel>()
    val channelState by channelViewModel.channelState.collectAsState()
    val messagesState by channelViewModel.messages.collectAsState()
    val connectionState by channelViewModel.connectionState.collectAsState()
    val isLoading by channelViewModel.isLoading.collectAsState()

    var showJoinDialog by remember { mutableStateOf(false) }
    var channelIdInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (connectionState == ConnectionState.DISCONNECTED) {
            channelViewModel.connect()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = channelState) {
            is ChannelState.Joined -> ChannelChatScreen(
                channel = state.channel,
                messages = messagesState,
                connectionState = connectionState,
                onReconnect = { channelViewModel.connect() },
                onSendMessage = { content -> channelViewModel.sendMessage(content) },
                onLeaveChannel = {
                    channelViewModel.leaveChannel()
                    onBack()
                },
                formatTimestamp = { channelViewModel.formatTimestamp(it) },
                isMessageCurrentUser = channelViewModel::isMessageCurrentUser
            )

            else -> ChannelSelectionScreen(
                onJoinChannel = { channelId ->
                    channelViewModel.joinChannel(channelId)
                    showJoinDialog = false
                },
                onCreateChannel = { showJoinDialog = true },
                isLoading = isLoading,
                connectionState = connectionState,
                onReconnect = { channelViewModel.connect() }
            )
        }

        if (showJoinDialog) {
            JoinChannelDialog(
                onJoin = { channelId ->
                    channelIdInput = channelId
                    channelViewModel.joinChannel(channelId)
                    showJoinDialog = false
                },
                onDismiss = { showJoinDialog = false },
                currentInput = channelIdInput,
                onInputChanged = { channelIdInput = it }
            )
        }
    }
}

@Composable
private fun ChannelSelectionScreen(
    onJoinChannel: (String) -> Unit,
    onCreateChannel: () -> Unit,
    isLoading: Boolean,
    connectionState: ConnectionState,
    onReconnect: () -> Unit
) {
    val palette = LocalPalette.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.group_24px),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = palette.accent
            )

            Text(
                text = stringResource(Res.string.together_title),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.textPrimary,
                modifier = Modifier.padding(top = 14.dp)
            )
            Text(
                text = stringResource(Res.string.together_description),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            ConnectionStatusIndicator(
                connectionState = connectionState,
                onReconnect = onReconnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            Button(
                onClick = { onJoinChannel(DEFAULT_CHANNEL_ID) },
                enabled = !isLoading,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            ) {
                Text(stringResource(Res.string.together_join_default_channel))
            }

            TextButton(
                onClick = onCreateChannel,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(stringResource(Res.string.together_create_channel))
            }
        }
    }
}

@Composable
private fun ChannelChatScreen(
    channel: Channel,
    messages: List<ChannelMessage>,
    connectionState: ConnectionState,
    onReconnect: () -> Unit,
    onSendMessage: (String) -> Unit,
    onLeaveChannel: () -> Unit,
    formatTimestamp: (Instant) -> String,
    isMessageCurrentUser: (ChannelMessage) -> Boolean
) {
    val palette = LocalPalette.current
    var messageInput by remember { mutableStateOf(TextFieldValue("")) }
    var showMemberList by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLeaveChannel) {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_back_24px),
                        contentDescription = "Back",
                        tint = palette.textPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${channel.members.size} members online",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                IconButton(onClick = { showMemberList = true }) {
                    Icon(
                        painter = painterResource(Res.drawable.group_24px),
                        contentDescription = "Members",
                        tint = palette.textSecondary
                    )
                }
            }
        }

        ConnectionStatusIndicator(
            connectionState = connectionState,
            onReconnect = onReconnect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            showDetails = false
        )

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        ) {
            Column {
                Box(modifier = Modifier.weight(1f)) {
                    ColumnList(
                        list = messages,
                        reverseLayout = true,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MessageItem(
                            message = it, formatTimestamp = formatTimestamp,
                            isMessageCurrentUser = isMessageCurrentUser
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        maxLines = 5,
                        placeholder = { Text(stringResource(Res.string.together_message_hint)) },
                        modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                            when {
                                event.type != KeyEventType.KeyDown -> false
                                event.key != Key.Enter && event.key != Key.NumPadEnter -> false
                                event.isShiftPressed -> {
                                    val text = messageInput.text
                                    val selection = messageInput.selection

                                    val newText = StringBuilder(text)
                                        .insert(selection.min, "\n")
                                        .toString()

                                    messageInput = TextFieldValue(
                                        text = newText,
                                        selection = TextRange(selection.min + 1)
                                    )
                                    true
                                }
                                else -> {
                                    if (messageInput.text.isNotBlank()) {
                                        onSendMessage(messageInput.text.trim())
                                        messageInput = TextFieldValue("")
                                    }
                                    true
                                }
                            }
                        },

                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageInput.text.isNotBlank()) {
                                    onSendMessage(messageInput.text.trim())
                                    messageInput = TextFieldValue("")
                                }
                            }
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageInput.text.isNotBlank()) {
                                onSendMessage(messageInput.text.trim())
                                messageInput = TextFieldValue("")
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.send_24px),
                            contentDescription = "Send",
                            tint = palette.accent
                        )
                    }
                }
            }
        }
    }

    if (showMemberList) {
        MemberListDialog(members = channel.members, onDismiss = { showMemberList = false })
    }
}

@Composable
private fun MessageItem(
    message: ChannelMessage,
    formatTimestamp: (Instant) -> String,
    isMessageCurrentUser: (ChannelMessage) -> Boolean
) {
    val palette = LocalPalette.current
    when (message) {
        is ChannelMessage.Text -> {
            if (isMessageCurrentUser(message)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 8.dp).weight(1f)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                formatTimestamp(message.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Text(
                                message.sender.username,
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary
                            )
                        }
                        Surface(
                            modifier = Modifier.padding(top = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = palette.accent.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    MemberAvatar(message.sender.username.firstOrNull()?.toString())
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MemberAvatar(message.sender.username.firstOrNull()?.toString())
                    Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                message.sender.username,
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary
                            )
                            Text(
                                formatTimestamp(message.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                        Surface(
                            modifier = Modifier.padding(top = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                        ) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        is ChannelMessage.System,
        is ChannelMessage.PlayControl -> {
            val content = when (message) {
                is ChannelMessage.System -> message.content
                is ChannelMessage.PlayControl -> "${message.sender.username} ${message.event}"
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun JoinChannelDialog(
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit,
    currentInput: String,
    onInputChanged: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp), shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.together_join_channel),
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedTextField(
                    value = currentInput,
                    onValueChange = onInputChanged,
                    label = { Text(stringResource(Res.string.together_channel_id)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Button(
                        onClick = { onJoin(currentInput) },
                        enabled = currentInput.isNotBlank(),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(stringResource(Res.string.join))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberAvatar(word: String?) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(palette.accent.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = word ?: "U",
            style = MaterialTheme.typography.labelMedium,
            color = palette.textPrimary
        )
    }
}

@Composable
private fun MemberListDialog(
    members: List<User>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.together_members),
                    style = MaterialTheme.typography.titleLarge
                )

                ColumnList(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .height(240.dp),
                    list = members
                ) { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(member.username.firstOrNull()?.toString() ?: "U")
                        }
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(member.username, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (member.isOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (member.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(Res.string.close))
                    }
                }
            }
        }
    }
}

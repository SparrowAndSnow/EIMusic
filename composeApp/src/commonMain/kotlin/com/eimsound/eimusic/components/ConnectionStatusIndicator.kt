package com.eimsound.eimusic.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.together.ConnectionState
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.autorenew_24px
import eimusic.composeapp.generated.resources.check_circle_fill_24px
import eimusic.composeapp.generated.resources.connection_connected
import eimusic.composeapp.generated.resources.connection_connecting
import eimusic.composeapp.generated.resources.connection_disconnected
import eimusic.composeapp.generated.resources.connection_establishing
import eimusic.composeapp.generated.resources.connection_lost
import eimusic.composeapp.generated.resources.connection_reconnect
import eimusic.composeapp.generated.resources.connection_reconnecting
import eimusic.composeapp.generated.resources.connection_restoring
import eimusic.composeapp.generated.resources.connection_sync_available
import eimusic.composeapp.generated.resources.error_fill_24px
import eimusic.composeapp.generated.resources.refresh_24px
import eimusic.composeapp.generated.resources.sync_24px
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
    onReconnect: () -> Unit = {},
    showDetails: Boolean = true
) {
    val palette = LocalPalette.current

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = getConnectionBackgroundColor(connectionState)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = getConnectionIcon(connectionState),
                        contentDescription = null,
                        tint = getConnectionForegroundColor(connectionState),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = getConnectionStatusText(connectionState),
                        style = MaterialTheme.typography.labelLarge,
                        color = getConnectionForegroundColor(connectionState),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (connectionState == ConnectionState.DISCONNECTED) {
                    IconButton(onClick = onReconnect) {
                        Icon(
                            painterResource(Res.drawable.refresh_24px),
                            contentDescription = stringResource(Res.string.connection_reconnect),
                            tint = palette.accent
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }

            if (showDetails) {
                Text(
                    text = getConnectionDescription(connectionState),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun getConnectionBackgroundColor(state: ConnectionState): Color {
    return when (state) {
        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        ConnectionState.CONNECTING,
        ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f)
    }
}

@Composable
private fun getConnectionIcon(state: ConnectionState): Painter {
    return when (state) {
        ConnectionState.CONNECTED -> painterResource(Res.drawable.check_circle_fill_24px)
        ConnectionState.DISCONNECTED -> painterResource(Res.drawable.error_fill_24px)
        ConnectionState.CONNECTING -> painterResource(Res.drawable.sync_24px)
        ConnectionState.RECONNECTING -> painterResource(Res.drawable.autorenew_24px)
    }
}

@Composable
private fun getConnectionForegroundColor(state: ConnectionState): Color {
    return when (state) {
        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.error
        ConnectionState.CONNECTING,
        ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.secondary
    }
}

@Composable
private fun getConnectionStatusText(state: ConnectionState): String {
    return when (state) {
        ConnectionState.CONNECTED -> stringResource(Res.string.connection_connected)
        ConnectionState.DISCONNECTED -> stringResource(Res.string.connection_disconnected)
        ConnectionState.CONNECTING -> stringResource(Res.string.connection_connecting)
        ConnectionState.RECONNECTING -> stringResource(Res.string.connection_reconnecting)
    }
}

@Composable
private fun getConnectionDescription(state: ConnectionState): String {
    return when (state) {
        ConnectionState.CONNECTED -> stringResource(Res.string.connection_sync_available)
        ConnectionState.DISCONNECTED -> stringResource(Res.string.connection_lost)
        ConnectionState.CONNECTING -> stringResource(Res.string.connection_establishing)
        ConnectionState.RECONNECTING -> stringResource(Res.string.connection_restoring)
    }
}

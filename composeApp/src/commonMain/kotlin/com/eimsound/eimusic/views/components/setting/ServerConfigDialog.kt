package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.cancel
import eimusic.composeapp.generated.resources.config_server
import eimusic.composeapp.generated.resources.config_server_http_label
import eimusic.composeapp.generated.resources.config_server_ws_label
import eimusic.composeapp.generated.resources.config_sources_url_hint
import eimusic.composeapp.generated.resources.setting_save
import org.jetbrains.compose.resources.stringResource

@Composable
fun ServerConfigDialog(
    isVisible: Boolean,
    serverHttp: String,
    serverWs: String,
    onServerHttpChange: (String) -> Unit,
    onServerWsChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.config_server)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = serverHttp,
                    onValueChange = onServerHttpChange,
                    label = { Text(stringResource(Res.string.config_server_http_label)) },
                    placeholder = { Text(stringResource(Res.string.config_sources_url_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = serverWs,
                    onValueChange = onServerWsChange,
                    label = { Text(stringResource(Res.string.config_server_ws_label)) },
                    placeholder = { Text("wss://") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.setting_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

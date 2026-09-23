package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.cancel
import eimusic.composeapp.generated.resources.setting_proxy
import eimusic.composeapp.generated.resources.setting_proxy_enable_label
import eimusic.composeapp.generated.resources.setting_proxy_host_label
import eimusic.composeapp.generated.resources.setting_proxy_port_label
import eimusic.composeapp.generated.resources.setting_save
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProxyDialog(
    isVisible: Boolean,
    proxyHost: String,
    proxyPort: String,
    proxyEnabled: Boolean,
    onProxyHostChange: (String) -> Unit,
    onProxyPortChange: (String) -> Unit,
    onProxyEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.setting_proxy)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = proxyHost,
                    onValueChange = onProxyHostChange,
                    label = { Text(stringResource(Res.string.setting_proxy_host_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proxyPort,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() } || it.isEmpty()) {
                            onProxyPortChange(it)
                        }
                    },
                    label = { Text(stringResource(Res.string.setting_proxy_port_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(Res.string.setting_proxy_enable_label))
                    Switch(checked = proxyEnabled, onCheckedChange = onProxyEnabledChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.setting_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}
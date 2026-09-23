package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eimsound.eimusic.viewmodel.ProxyState
import com.eimsound.eimusic.views.SettingSection
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.setting_network
import eimusic.composeapp.generated.resources.setting_proxy
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingProxy(
    proxyState: ProxyState,
    updateProxy: (String, Int, Boolean) -> Unit
) {
    var proxyHost by remember { mutableStateOf(proxyState.proxyHost) }
    var proxyPort by remember { mutableStateOf(proxyState.proxyPort.toString()) }
    var proxyEnabled by remember { mutableStateOf(proxyState.proxyEnabled) }
    var showProxyDialog by remember { mutableStateOf(false) }

    SettingSection(title = stringResource(Res.string.setting_network)) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.setting_proxy)) },
            supportingContent = {
                Text("${proxyState.proxyHost}:${proxyState.proxyPort}")
            },
            trailingContent = {
                Switch(
                    checked = proxyState.proxyEnabled,
                    onCheckedChange = {
                        updateProxy(proxyState.proxyHost, proxyState.proxyPort, it)
                    }
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            modifier = Modifier.clickable {
                proxyHost = proxyState.proxyHost
                proxyPort = proxyState.proxyPort.toString()
                proxyEnabled = proxyState.proxyEnabled
                showProxyDialog = true
            }
        )
    }

    ProxyDialog(
        isVisible = showProxyDialog,
        proxyHost = proxyHost,
        proxyPort = proxyPort,
        proxyEnabled = proxyEnabled,
        onProxyHostChange = { proxyHost = it },
        onProxyPortChange = { proxyPort = it },
        onProxyEnabledChange = { proxyEnabled = it },
        onDismiss = { showProxyDialog = false },
        onConfirm = {
            updateProxy(proxyHost, proxyPort.toIntOrNull() ?: 8080, proxyEnabled)
            showProxyDialog = false
        }
    )
}
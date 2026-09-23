package com.eimsound.eimusic.views.components.setting

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.viewmodel.ConfigState
import com.eimsound.eimusic.views.SettingSection
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.config_section
import eimusic.composeapp.generated.resources.config_server
import eimusic.composeapp.generated.resources.config_server_http_label
import eimusic.composeapp.generated.resources.config_server_ws_label
import eimusic.composeapp.generated.resources.config_sources
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingConfig(
    configState: ConfigState,
    onSave: (String, String, Map<TrackSource, List<String>>) -> Boolean
) {
    var serverHttp by remember { mutableStateOf(configState.serverHttp) }
    var serverWs by remember { mutableStateOf(configState.serverWs) }
    var sources by remember { mutableStateOf(configState.sources) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showSourcesDialog by remember { mutableStateOf(false) }

    val sourceLabels = TrackSource.entries.associateWith { it.displayLabel() }
    val sourcesSummary = remember(configState.sources, sourceLabels) {
        configState.sources
            .filter { it.value.isNotEmpty() }
            .entries.joinToString(" · ") { "${sourceLabels[it.key] ?: it.key.name}: ${it.value.size}" }
    }
    val httpLabel = stringResource(Res.string.config_server_http_label)
    val wsLabel = stringResource(Res.string.config_server_ws_label)

    SettingSection(title = stringResource(Res.string.config_section)) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.config_server)) },
            supportingContent = { Text("$httpLabel: ${configState.serverHttp}\n$wsLabel: ${configState.serverWs}") },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable {
                serverHttp = configState.serverHttp
                serverWs = configState.serverWs
                showServerDialog = true
            }
        )
        ListItem(
            headlineContent = { Text(stringResource(Res.string.config_sources)) },
            supportingContent = { Text(sourcesSummary.ifEmpty { "-" }) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable {
                sources = configState.sources
                showSourcesDialog = true
            }
        )
    }

    ServerConfigDialog(
        isVisible = showServerDialog,
        serverHttp = serverHttp,
        serverWs = serverWs,
        onServerHttpChange = { serverHttp = it },
        onServerWsChange = { serverWs = it },
        // 取消时丢弃本对话框的编辑，避免被另一个对话框的保存连带提交
        onDismiss = {
            serverHttp = configState.serverHttp
            serverWs = configState.serverWs
            showServerDialog = false
        },
        onConfirm = {
            if (onSave(serverHttp, serverWs, sources)) showServerDialog = false
        }
    )
    SourceListDialog(
        isVisible = showSourcesDialog,
        sources = sources,
        onSourcesChange = { sources = it },
        onDismiss = {
            sources = configState.sources
            showSourcesDialog = false
        },
        onConfirm = {
            if (onSave(serverHttp, serverWs, sources)) showSourcesDialog = false
        }
    )
}

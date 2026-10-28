package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.config.AppConfig
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.ui.components.InputField
import com.eimsound.eimusic.ui.components.ListView
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.components.rememberListSelectionState
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Text

private enum class SettingEdit { Http, Ws, Source, Proxy }

private data class SettingRow(
    val label: String,
    val value: String,
    val action: () -> Unit,
)

@Composable
fun SettingsScreen(
    storage: IStorage,
    localViewModel: LocalViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onOpenLocalPaths: () -> Unit,
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val terminal = LocalTerminalState.current
    val selection = rememberListSelectionState()
    val localState by localViewModel.state.collectAsState()

    var config by remember { mutableStateOf(storage.get(AppConfig())) }
    var proxyEnabled by remember { mutableStateOf(storage.get(Settings::proxyEnabled, false)) }
    var proxyHost by remember { mutableStateOf(storage.get(Settings::proxyHost, "127.0.0.1")) }
    var proxyPort by remember { mutableStateOf(storage.get(Settings::proxyPort, 8080)) }
    var editing by remember { mutableStateOf<SettingEdit?>(null) }
    var editValue by remember { mutableStateOf("") }
    var editPlaceholder by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }

    fun persistConfig(update: (AppConfig) -> AppConfig) {
        val next = update(config)
        config = next
        storage.save(next)
        notice = "已保存，重启后生效"
    }

    fun persistProxy(enabled: Boolean, host: String, port: Int) {
        proxyEnabled = enabled
        proxyHost = host
        proxyPort = port
        storage.save(Settings::proxyEnabled, enabled)
        storage.save(Settings::proxyHost, host)
        storage.save(Settings::proxyPort, port)
        notice = "已保存，重启后生效"
    }

    fun startEdit(field: SettingEdit, value: String) {
        editing = field
        editValue = ""
        editPlaceholder = value
        notice = null
    }

    fun commitEdit() {
        if (editValue.isBlank()) {
            editing = null
            return
        }
        when (editing) {
            SettingEdit.Http -> persistConfig {
                it.copy(server = it.server.copy(http = editValue.trim()))
            }
            SettingEdit.Ws -> persistConfig {
                it.copy(server = it.server.copy(ws = editValue.trim()))
            }
            SettingEdit.Source -> {
                val urls = editValue.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                persistConfig { it.copy(sources = it.sources + (TrackSource.NETEASE to urls)) }
            }
            SettingEdit.Proxy -> {
                val host = editValue.substringBeforeLast(':', editValue).trim()
                val port = editValue.substringAfterLast(':', "").toIntOrNull() ?: proxyPort
                persistProxy(proxyEnabled, host.ifBlank { proxyHost }, port)
            }
            null -> Unit
        }
        editing = null
    }

    val sourceSummary = config.sources[TrackSource.NETEASE].orEmpty().joinToString(", ").ifBlank { "-" }
    val rows = listOf(
        SettingRow("主题", if (isDark) "暗色" else "亮色", onToggleTheme),
        SettingRow("服务器 HTTP", config.server.http, { startEdit(SettingEdit.Http, config.server.http) }),
        SettingRow("服务器 WS", config.server.ws, { startEdit(SettingEdit.Ws, config.server.ws) }),
        SettingRow("网易音源", sourceSummary, { startEdit(SettingEdit.Source, sourceSummary) }),
        SettingRow(
            "代理",
            if (proxyEnabled) "开启" else "关闭",
            {
                persistProxy(!proxyEnabled, proxyHost, proxyPort)
            },
        ),
        SettingRow("代理地址", "$proxyHost:$proxyPort", {
            startEdit(SettingEdit.Proxy, "$proxyHost:$proxyPort")
        }),
        SettingRow("本地音乐", "${localState.localPaths.size} 个路径", onOpenLocalPaths),
        SettingRow("关于", "EIMusic TUI", {}),
    )

    ScreenFrame(
        hint = if (editing == null) "j/k:选择  Enter:编辑  Esc:返回" else "Enter:保存  Esc:取消",
        onKey = { event ->
            if (editing == null && event.key == "Escape") {
                onGoBack()
                true
            } else {
                false
            }
        },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val contentWidth = (terminal.size.columns * 0.7f).toInt().coerceIn(50, 90)
            Column(Modifier.width(contentWidth).padding(2)) {
                Text(" Settings", color = palette.titleFg)
                Text(" 终端设置", color = palette.textDimFg)
                Text("", color = palette.textFg)

                val edit = editing
                if (edit != null) {
                    val title = when (edit) {
                        SettingEdit.Http -> "服务器 HTTP"
                        SettingEdit.Ws -> "服务器 WS"
                        SettingEdit.Source -> "网易音源（逗号分隔）"
                        SettingEdit.Proxy -> "代理地址（host:port）"
                    }
                    Text(" 编辑 $title", color = palette.titleFg)
                    InputField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        placeholder = editPlaceholder.ifBlank { title },
                        onConfirm = { commitEdit() },
                        onCancel = { editing = null },
                    )
                } else {
                    ListView(
                        items = rows,
                        visibleLines = (terminal.size.rows - 6).coerceAtLeast(1),
                        emptyMessage = "",
                        confirmOnSingleClick = true,
                        selection = selection,
                        onConfirm = { it.action() },
                        render = { row, _, selected ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    (if (selected) " ▸ " else "   ") + row.label,
                                    modifier = Modifier.weight(1f),
                                    color = if (selected) palette.highlightFg else palette.textFg,
                                )
                                Text("  " + row.value + " ", color = palette.textDimFg)
                            }
                        },
                    )
                }
                notice?.let { Text("  $it", color = palette.highlightFg) }
            }
        }
    }
}

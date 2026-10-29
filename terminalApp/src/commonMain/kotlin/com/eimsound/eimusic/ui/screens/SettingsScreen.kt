package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.config.AppConfig
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.ui.components.InputField
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.clickable
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
import com.jakewharton.mosaic.ui.TextOverflow

private sealed interface SettingEdit {
    data object Http : SettingEdit
    data object Ws : SettingEdit
    data object Proxy : SettingEdit
    data class Source(val source: TrackSource) : SettingEdit
}

private data class SettingRow(
    val label: String,
    val value: String,
    val action: () -> Unit,
)

private data class SettingSection(
    val title: String,
    val rows: List<SettingRow>,
)

private const val SAVED_NOTICE = "已保存，重启后生效"

private val editableSources = TrackSource.entries.filter {
    it != TrackSource.LOCAL && it != TrackSource.UNKNOWN
}

private fun handleListKey(
    event: KeyEvent,
    selected: Int,
    lastIndex: Int,
    onSelect: (Int) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
): Boolean = when (event.key) {
    "j", "ArrowDown" -> {
        onSelect((selected + 1).coerceAtMost(lastIndex.coerceAtLeast(0)))
        true
    }
    "k", "ArrowUp" -> {
        onSelect((selected - 1).coerceAtLeast(0))
        true
    }
    "Enter" -> {
        onConfirm()
        true
    }
    "Escape" -> {
        onBack()
        true
    }
    else -> false
}

private fun sourceLabel(source: TrackSource): String = when (source) {
    TrackSource.NETEASE -> "网易云"
    TrackSource.SPOTIFY -> "Spotify"
    TrackSource.QQ -> "QQ 音乐"
    TrackSource.XIAMI -> "虾米"
    TrackSource.KUWO -> "酷我"
    TrackSource.KUGOU -> "酷狗"
    TrackSource.BAIDU -> "百度"
    TrackSource.YOUTUBE -> "YouTube"
    TrackSource.YOUTUBE_MUSIC -> "YouTube Music"
    else -> source.name
}

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
    val localState by localViewModel.state.collectAsState()

    var config by remember { mutableStateOf(storage.get(AppConfig())) }
    var proxyEnabled by remember { mutableStateOf(storage.get(Settings::proxyEnabled, false)) }
    var proxyHost by remember { mutableStateOf(storage.get(Settings::proxyHost, "127.0.0.1")) }
    var proxyPort by remember { mutableStateOf(storage.get(Settings::proxyPort, 8080)) }
    var editing by remember { mutableStateOf<SettingEdit?>(null) }
    var editValue by remember { mutableStateOf("") }
    var editPlaceholder by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableIntStateOf(0) }
    var sourcePage by remember { mutableStateOf(false) }
    var sourceSelected by remember { mutableIntStateOf(0) }

    fun persistConfig(update: (AppConfig) -> AppConfig) {
        val next = update(config)
        config = next
        storage.save(next)
        notice = SAVED_NOTICE
    }

    fun persistProxy(enabled: Boolean, host: String, port: Int) {
        proxyEnabled = enabled
        proxyHost = host
        proxyPort = port
        storage.save(Settings::proxyEnabled, enabled)
        storage.save(Settings::proxyHost, host)
        storage.save(Settings::proxyPort, port)
        notice = SAVED_NOTICE
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
        when (val field = editing) {
            SettingEdit.Http -> persistConfig { it.copy(server = it.server.copy(http = editValue.trim())) }
            SettingEdit.Ws -> persistConfig { it.copy(server = it.server.copy(ws = editValue.trim())) }
            is SettingEdit.Source -> {
                val urls = editValue.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                persistConfig { it.copy(sources = it.sources + (field.source to urls)) }
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

    val sections = listOf(
        SettingSection("外观", listOf(
            SettingRow("主题", if (isDark) "暗色" else "亮色", onToggleTheme),
        )),
        SettingSection("网络", listOf(
            SettingRow("服务器 HTTP", config.server.http, { startEdit(SettingEdit.Http, config.server.http) }),
            SettingRow("服务器 WS", config.server.ws, { startEdit(SettingEdit.Ws, config.server.ws) }),
            SettingRow("代理", if (proxyEnabled) "开启" else "关闭", {
                persistProxy(!proxyEnabled, proxyHost, proxyPort)
            }),
            SettingRow("代理地址", "$proxyHost:$proxyPort", {
                startEdit(SettingEdit.Proxy, "$proxyHost:$proxyPort")
            }),
        )),
        SettingSection("音源", listOf(
            SettingRow(
                "音源配置",
                "${editableSources.count { config.sources[it].orEmpty().isNotEmpty() }} 个已配置",
                { sourcePage = true },
            ),
        )),
        SettingSection("本地", listOf(
            SettingRow("音乐路径", "${localState.localPaths.size} 个路径", onOpenLocalPaths),
        )),
        SettingSection("关于", listOf(
            SettingRow("版本", "EIMusic TUI", {}),
        )),
    )
    val flatRows = sections.flatMap { it.rows }
    val sourceRows = editableSources.map { source ->
        val urls = config.sources[source].orEmpty()
        SettingRow(
            sourceLabel(source),
            if (urls.isEmpty()) "未配置" else "${urls.size} 个地址",
            { startEdit(SettingEdit.Source(source), urls.joinToString(", ")) },
        )
    }
    LaunchedEffect(flatRows.size) {
        selected = selected.coerceIn(0, (flatRows.size - 1).coerceAtLeast(0))
    }
    LaunchedEffect(sourceRows.size) {
        sourceSelected = sourceSelected.coerceIn(0, (sourceRows.size - 1).coerceAtLeast(0))
    }

    ScreenFrame(
        hint = when {
            editing != null -> "Enter:保存  Esc:取消"
            sourcePage -> "j/k:选择  Enter:编辑  Esc:返回"
            else -> "j/k:选择  Enter:打开  Esc:返回"
        },
        onKey = { event ->
            if (editing != null) {
                false
            } else if (sourcePage) {
                handleListKey(
                    event = event,
                    selected = sourceSelected,
                    lastIndex = sourceRows.lastIndex,
                    onSelect = { sourceSelected = it },
                    onConfirm = { sourceRows.getOrNull(sourceSelected)?.action?.invoke() },
                    onBack = { sourcePage = false },
                )
            } else {
                handleListKey(
                    event = event,
                    selected = selected,
                    lastIndex = flatRows.lastIndex,
                    onSelect = { selected = it },
                    onConfirm = { flatRows.getOrNull(selected)?.action?.invoke() },
                    onBack = onGoBack,
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val contentWidth = (terminal.size.columns * 0.72f).toInt().coerceIn(54, 92)
            Column(Modifier.width(contentWidth).padding(horizontal = 2, vertical = 1)) {
                Text(" Settings", color = palette.titleFg)
                Text(" 终端设置", color = palette.textDimFg)
                Text("", color = palette.textFg)

                val edit = editing
                if (edit != null) {
                    SettingEditView(
                        edit = edit,
                        value = editValue,
                        placeholder = editPlaceholder,
                        onValueChange = { editValue = it },
                        onConfirm = { commitEdit() },
                        onCancel = { editing = null },
                    )
                } else if (sourcePage) {
                    SettingSectionHeader("音源", contentWidth)
                    sourceRows.forEachIndexed { index, row ->
                        SettingRowView(
                            row = row,
                            selected = index == sourceSelected,
                            onClick = {
                                sourceSelected = index
                                row.action()
                            },
                        )
                    }
                    Text("", color = palette.textFg)
                } else {
                    var rowIndex = 0
                    sections.forEach { section ->
                        SettingSectionHeader(section.title, contentWidth)
                        section.rows.forEach { row ->
                            val index = rowIndex++
                            SettingRowView(
                                row = row,
                                selected = index == selected,
                                onClick = {
                                    selected = index
                                    row.action()
                                },
                            )
                        }
                        Text("", color = palette.textFg)
                    }
                }
                notice?.let { Text("  $it", color = palette.highlightFg) }
            }
        }
    }
}

@Composable
private fun SettingSectionHeader(title: String, contentWidth: Int) {
    val palette = LocalTuiColors.current
    Text("  $title", color = palette.titleFg)
    Text(
        "  " + "─".repeat((contentWidth - 6).coerceAtLeast(10)),
        color = palette.dimBorderFg,
    )
}

@Composable
private fun SettingEditView(
    edit: SettingEdit,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val title = when (edit) {
        SettingEdit.Http -> "服务器 HTTP"
        SettingEdit.Ws -> "服务器 WS"
        SettingEdit.Proxy -> "代理地址（host:port）"
        is SettingEdit.Source -> sourceLabel(edit.source) + " 音源"
    }
    Text(" 编辑 $title", color = palette.titleFg)
    Text(" 当前: $placeholder", color = palette.textDimFg)
    InputField(
        value = value,
        onValueChange = onValueChange,
        placeholder = "输入新值，回车保存",
        onConfirm = onConfirm,
        onCancel = onCancel,
    )
}

@Composable
private fun SettingRowView(
    row: SettingRow,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalTuiColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(
            if (selected) " ▸ " else "   ",
            color = if (selected) palette.highlightFg else palette.textDimFg,
        )
        Text(
            row.label,
            modifier = Modifier.weight(1f),
            color = if (selected) palette.highlightFg else palette.textFg,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "  " + row.value + " ",
            color = palette.textDimFg,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

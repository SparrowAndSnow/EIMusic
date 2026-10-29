package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.TerminalVersion
import com.eimsound.eimusic.config.AppConfig
import com.eimsound.eimusic.i18n.I18n
import com.eimsound.eimusic.i18n.Language
import com.eimsound.eimusic.i18n.t
import com.eimsound.eimusic.keyboard.SettingFocus
import com.eimsound.eimusic.keyboard.SettingKeyActions
import com.eimsound.eimusic.keyboard.dispatchSettingKey
import com.eimsound.eimusic.keyboard.settingStatusBarHint
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.network.UpdateApi
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.ui.components.InputField
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.LocalTerminalState
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
import kotlinx.coroutines.launch

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

private sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class UpToDate(val version: String) : UpdateStatus
    data class Available(val version: String, val url: String) : UpdateStatus
    data class Failed(val message: String) : UpdateStatus
}

private val editableSources = TrackSource.entries.filter {
    it != TrackSource.LOCAL && it != TrackSource.UNKNOWN
}

private fun sourceLabel(source: TrackSource): String = when (source) {
    TrackSource.NETEASE -> t("source.netease")
    TrackSource.SPOTIFY -> t("source.spotify")
    TrackSource.QQ -> t("source.qq")
    TrackSource.XIAMI -> t("source.xiami")
    TrackSource.KUWO -> t("source.kuwo")
    TrackSource.KUGOU -> t("source.kugou")
    TrackSource.BAIDU -> t("source.baidu")
    TrackSource.YOUTUBE -> t("source.youtube")
    TrackSource.YOUTUBE_MUSIC -> t("source.youtubeMusic")
    else -> source.name
}

@Composable
fun SettingsScreen(
    storage: IStorage,
    updateApi: UpdateApi,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val terminal = LocalTerminalState.current

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
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    val scope = rememberCoroutineScope()

    fun checkUpdate() {
        if (updateStatus == UpdateStatus.Checking) return
        updateStatus = UpdateStatus.Checking
        scope.launch {
            val result = updateApi.checkUpdate(TerminalVersion.VERSION, PlatformType.Native, "linux")
            updateStatus = when (result) {
                is NetworkResult.Success -> {
                    val data = result.data
                    if (data.hasUpdate) {
                        UpdateStatus.Available(
                            version = data.latestVersion ?: data.update?.version ?: "?",
                            url = data.update?.downloadUrl.orEmpty(),
                        )
                    } else {
                        UpdateStatus.UpToDate(data.latestVersion ?: TerminalVersion.VERSION)
                    }
                }
                is NetworkResult.Error -> UpdateStatus.Failed(result.message)
                is NetworkResult.Exception -> UpdateStatus.Failed(result.message ?: "unknown")
            }
        }
    }

    fun persistConfig(update: (AppConfig) -> AppConfig) {
        val next = update(config)
        config = next
        storage.save(next)
        notice = t("settings.saved")
    }

    fun persistProxy(enabled: Boolean, host: String, port: Int) {
        proxyEnabled = enabled
        proxyHost = host
        proxyPort = port
        storage.save(Settings::proxyEnabled, enabled)
        storage.save(Settings::proxyHost, host)
        storage.save(Settings::proxyPort, port)
        notice = t("settings.saved")
    }

    fun switchLanguage() {
        val next = if (I18n.language == Language.Chinese) Language.English else Language.Chinese
        I18n.setLanguage(next)
        storage.save(Settings::language, next.code)
        notice = t("settings.saved")
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
        SettingSection(
            t("settings.appearance"),
            listOf(
                SettingRow(
                    t("settings.theme"),
                    if (isDark) t("settings.theme.dark") else t("settings.theme.light"),
                    onToggleTheme,
                ),
                SettingRow(
                    t("settings.language"),
                    if (I18n.language == Language.Chinese) t("settings.language.zh") else t("settings.language.en"),
                    { switchLanguage() },
                ),
            ),
        ),
        SettingSection(
            t("settings.network"),
            listOf(
                SettingRow(t("settings.serverHttp"), config.server.http, {
                    startEdit(SettingEdit.Http, config.server.http)
                }),
                SettingRow(t("settings.serverWs"), config.server.ws, {
                    startEdit(SettingEdit.Ws, config.server.ws)
                }),
                SettingRow(
                    t("settings.proxy"),
                    if (proxyEnabled) t("settings.proxy.on") else t("settings.proxy.off"),
                    { persistProxy(!proxyEnabled, proxyHost, proxyPort) },
                ),
                SettingRow(t("settings.proxyAddress"), "$proxyHost:$proxyPort", {
                    startEdit(SettingEdit.Proxy, "$proxyHost:$proxyPort")
                }),
            ),
        ),
        SettingSection(
            t("settings.sources"),
            listOf(
                SettingRow(
                    t("settings.sourcesConfig"),
                    t(
                        "settings.sourcesConfigured",
                        editableSources.count { config.sources[it].orEmpty().isNotEmpty() },
                    ),
                    { sourcePage = true },
                ),
            ),
        ),
        SettingSection(
            t("settings.about"),
            buildList {
                add(SettingRow(t("settings.version"), "EIMusic TUI v" + TerminalVersion.VERSION, {}))
                add(
                    SettingRow(
                        t("settings.update"),
                        when (val status = updateStatus) {
                            UpdateStatus.Idle -> t("settings.update.idle")
                            UpdateStatus.Checking -> t("settings.update.checking")
                            is UpdateStatus.UpToDate -> t("settings.update.upToDate", status.version)
                            is UpdateStatus.Available -> t("settings.update.available", status.version)
                            is UpdateStatus.Failed -> t("settings.update.failed") + ": " + status.message
                        },
                        { checkUpdate() },
                    ),
                )
                val status = updateStatus
                if (status is UpdateStatus.Available && status.url.isNotBlank()) {
                    add(
                        SettingRow(
                            t("settings.update.download"),
                            status.url,
                            { notice = status.url },
                        ),
                    )
                }
            },
        ),
    )
    val flatRows = sections.flatMap { it.rows }
    val sourceRows = editableSources.map { source ->
        val urls = config.sources[source].orEmpty()
        SettingRow(
            sourceLabel(source),
            if (urls.isEmpty()) t("settings.sourceUnset") else t("settings.sourceUrls", urls.size),
            { startEdit(SettingEdit.Source(source), urls.joinToString(", ")) },
        )
    }
    LaunchedEffect(flatRows.size) {
        selected = selected.coerceIn(0, (flatRows.size - 1).coerceAtLeast(0))
    }
    LaunchedEffect(sourceRows.size) {
        sourceSelected = sourceSelected.coerceIn(0, (sourceRows.size - 1).coerceAtLeast(0))
    }

    val focus = when {
        editing != null -> SettingFocus.Editing
        sourcePage -> SettingFocus.Sources
        else -> SettingFocus.Main
    }

    ScreenFrame(
        hint = settingStatusBarHint(focus),
        onKey = { event ->
            dispatchSettingKey(
                event = event,
                focus = focus,
                actions = SettingKeyActions(
                    moveSelection = { delta ->
                        if (focus == SettingFocus.Sources) {
                            sourceSelected = (sourceSelected + delta)
                                .coerceIn(0, sourceRows.lastIndex.coerceAtLeast(0))
                        } else {
                            selected = (selected + delta)
                                .coerceIn(0, flatRows.lastIndex.coerceAtLeast(0))
                        }
                    },
                    confirm = {
                        if (focus == SettingFocus.Sources) {
                            sourceRows.getOrNull(sourceSelected)?.action?.invoke()
                        } else {
                            flatRows.getOrNull(selected)?.action?.invoke()
                        }
                    },
                    back = {
                        if (focus == SettingFocus.Sources) sourcePage = false else onGoBack()
                    },
                ),
            )
        },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val contentWidth = (terminal.size.columns * 0.72f).toInt().coerceIn(54, 92)
            Column(Modifier.width(contentWidth).padding(horizontal = 2, vertical = 1)) {
                Text(" " + t("settings.title"), color = palette.titleFg)
                Text(" " + t("settings.subtitle"), color = palette.textDimFg)
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
                    SettingSectionHeader(t("settings.sources"), contentWidth)
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
        SettingEdit.Http -> t("settings.serverHttp")
        SettingEdit.Ws -> t("settings.serverWs")
        SettingEdit.Proxy -> t("settings.proxyAddress")
        is SettingEdit.Source -> sourceLabel(edit.source)
    }
    Text(" " + t("settings.edit", title), color = palette.titleFg)
    Text(" " + t("settings.current", placeholder), color = palette.textDimFg)
    InputField(
        value = value,
        onValueChange = onValueChange,
        placeholder = t("settings.placeholder"),
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

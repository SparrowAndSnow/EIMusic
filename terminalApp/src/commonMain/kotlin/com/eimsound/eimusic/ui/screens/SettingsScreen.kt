package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.name
import com.eimsound.eimusic.ui.components.ListView
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.components.rememberListSelectionState
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.viewmodel.AuthViewModel
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
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
import com.jakewharton.mosaic.LocalTerminalState

private data class SettingRow(
    val label: String,
    val value: String,
    val action: () -> Unit,
)

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    localViewModel: LocalViewModel,
    playerViewModel: PlayerViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenLocalPaths: () -> Unit,
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val safeArea = LocalSafeContentSize.current
    val termWidth = LocalTerminalState.current.size.columns
    val contentWidth = (termWidth * 0.7f).toInt().coerceIn(40, 80)
    val selection = rememberListSelectionState()
    val authState by authViewModel.authState.collectAsState()
    val localState by localViewModel.state.collectAsState()
    val playerState by playerViewModel.state.collectAsState()

    val loggedIn = authState is AuthState.Authenticated
    val account = when (val state = authState) {
        is AuthState.Authenticated -> state.user.username
        is AuthState.Loading -> "..."
        else -> "未登录"
    }
    val playMode = when (playerState.playMode) {
        is PlayMode.Loop -> "循环"
        is PlayMode.RepeatOne -> "单曲"
        is PlayMode.Shuffle -> "随机"
    }

    val rows = listOf(
        SettingRow("主题", if (isDark) "暗色" else "亮色", onToggleTheme),
        SettingRow("播放模式", playMode) { playerViewModel.togglePlayMode() },
        SettingRow("账号", account) {
            if (loggedIn) authViewModel.logout() else onOpenLogin()
        },
        SettingRow("本地音乐", "${localState.localPaths.size} 个路径", onOpenLocalPaths),
        SettingRow("关于", "EIMusic TUI", {}),
    )

    ScreenFrame(
        hint = "j/k:选择  Enter:调整  Esc:返回",
        onKey = { event ->
            if (event.key == "Escape") {
                onGoBack()
                true
            } else {
                false
            }
        },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.width(contentWidth).padding(2)) {
            Text(" Settings", color = palette.titleFg)
            Text(" 终端设置", color = palette.textDimFg)
            Text("", color = palette.textFg)
            ListView(
                items = rows,
                visibleLines = (safeArea.rows - 6).coerceAtLeast(1),
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
        }
    }
}

package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.i18n.t
import com.eimsound.eimusic.keyboard.LocalGlobalActions
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.ui.Icon
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.bridgeToMutable
import com.eimsound.eimusic.viewmodel.AuthViewModel
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.clickable
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.MouseEvent
import com.jakewharton.mosaic.layout.hoverable
import com.jakewharton.mosaic.layout.onMouseEvent
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.BoxWithConstraints
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.RowScope
import com.jakewharton.mosaic.ui.Spacer
import com.jakewharton.mosaic.ui.Text

@Composable
fun HeaderBar(
    title: String,
    state: PlayerState,
    authViewModel: AuthViewModel,
    isDark: Boolean,
    onNavigate: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenTogether: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTheme: () -> Unit,
    channelLabel: String? = null,
) {
    val palette = LocalTuiColors.current
    val globalActions = LocalGlobalActions.current
    val authState by authViewModel.authState.bridgeToMutable()
    val authLabel = when (val s = authState) {
        is AuthState.Authenticated -> s.user.username
        is AuthState.Loading -> "..."
        else -> t("header.login")
    }.take(18)
    val queue = "${state.queue.size}"
    val themeIcon = if (isDark) Icon.Moon.sym else Icon.Sun.sym
    val controls = playControlData(state)
    val trackText = state.current?.let { track ->
        val artist = track.artists?.firstOrNull()?.name.orEmpty()
        if (artist.isBlank()) track.name else "${track.name} — $artist"
    } ?: t("main.noTrack")

    Row(
        modifier = Modifier.fillMaxWidth().background(palette.headerBg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(" $title", color = palette.titleFg)
        Spacer(Modifier.width(2))
        BoxWithConstraints(Modifier.weight(1f)) {
            if (maxWidth >= 8) MarqueeText(trackText, maxWidth)
        }
        Spacer(Modifier.width(2))
        HeaderButton(onToggleTheme) {
            Text(themeIcon, color = palette.modeFg)
        }
        Spacer(Modifier.width(1))
        HeaderButton(globalActions.onTogglePlayMode) {
            Text(controls.modeIcon, color = palette.modeFg)
        }
        Spacer(Modifier.width(1))
        HeaderButton(globalActions.onPlayPause) {
            Text(controls.playIcon, color = palette.playBtnFg)
        }
        Spacer(Modifier.width(1))
        HeaderButton(
            onClick = globalActions.onToggleMute,
            onWheel = { up -> globalActions.onVolumeScroll(if (up) 1 else -1) },
        ) {
            Text(controls.volIcon, color = palette.textFg)
            Spacer(Modifier.width(1))
            Text(controls.volPercent, color = palette.textDimFg)
        }
        Spacer(Modifier.width(2))
        HeaderButton(onOpenQueue) {
            Text(Icon.Queue.sym, color = palette.textDimFg)
            Spacer(Modifier.width(1))
            Text(queue, color = palette.textFg)
        }
        Spacer(Modifier.width(2))
        HeaderButton(onOpenSearch) {
            Text(Icon.Search.sym, color = palette.textDimFg)
        }
        Spacer(Modifier.width(2))
        HeaderButton(onOpenSettings) {
            Text(Icon.Settings.sym, color = palette.textDimFg)
        }
        Spacer(Modifier.width(2))
        HeaderButton(onNavigate) {
            Text(Icon.User.sym, color = palette.textDimFg)
            Spacer(Modifier.width(1))
            Text(authLabel, color = palette.highlightFg)
        }
        if (channelLabel != null) {
            Spacer(Modifier.width(2))
            HeaderButton(onOpenTogether) {
                Text(Icon.Together.sym, color = palette.highlightFg)
                Spacer(Modifier.width(1))
                Text(channelLabel.take(24), color = palette.highlightFg)
            }
        }
    }
}

@Composable
private fun MarqueeText(text: String, width: Int) {
    val palette = LocalTuiColors.current
    val loop = text + "   "
    val loopCells = stringCells(loop)
    val scrolls = stringCells(text) > width
    var offset by remember(text) { mutableIntStateOf(0) }

    LaunchedEffect(text, scrolls, loopCells) {
        if (!scrolls || loopCells <= 0) return@LaunchedEffect
        while (true) {
            delay(150)
            offset = (offset + 1) % loopCells
        }
    }

    val shown = if (!scrolls) {
        text
    } else {
        marqueeWindow(loop, offset, width)
    }
    Text(
        shown,
        modifier = Modifier.width(width),
        color = palette.textDimFg,
        softWrap = false,
        maxLines = 1,
    )
}

/** 终端单元格宽度：CJK / 全角字符按 2 格计算。 */
private fun charCells(c: Char): Int {
    val code = c.code
    return if (
        code in 0x1100..0x115F ||
        code in 0x2E80..0xA4CF ||
        code in 0xAC00..0xD7A3 ||
        code in 0xF900..0xFAFF ||
        code in 0xFE30..0xFE4F ||
        code in 0xFF00..0xFF60 ||
        code in 0xFFE0..0xFFE6
    ) 2 else 1
}

private fun stringCells(s: String): Int = s.sumOf { charCells(it) }

/** 从 [start] 格开始截取 [width] 格，用于跑马灯窗口。 */
private fun marqueeWindow(loop: String, start: Int, width: Int): String {
    if (loop.isEmpty() || width <= 0) return ""
    val total = stringCells(loop)
    var remaining = start % total
    var index = 0
    while (remaining > 0) {
        remaining -= charCells(loop[index])
        index++
    }
    val sb = StringBuilder()
    var cells = 0
    while (cells < width) {
        val c = loop[index % loop.length]
        val w = charCells(c)
        if (cells + w > width) break
        sb.append(c)
        cells += w
        index++
    }
    while (cells < width) {
        sb.append(' ')
        cells++
    }
    return sb.toString()
}

@Composable
private fun HeaderButton(
    onClick: () -> Unit,
    onWheel: ((up: Boolean) -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalTuiColors.current
    var hovered by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .hoverable { hovered = it }
            .background(if (hovered) palette.hoverBg else Color.Unspecified)
            .then(
                if (onWheel == null) {
                    Modifier
                } else {
                    Modifier.onMouseEvent { event ->
                        if (event.type == MouseEvent.Type.Press &&
                            (event.button == MouseEvent.Button.WheelUp || event.button == MouseEvent.Button.WheelDown)
                        ) {
                            onWheel(event.button == MouseEvent.Button.WheelUp)
                            true
                        } else {
                            false
                        }
                    }
                },
            )
            .padding(horizontal = 1),
        content = content,
    )
}

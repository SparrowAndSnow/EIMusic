package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.i18n.t
import com.eimsound.eimusic.keyboard.LocalGlobalActions
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.ui.Icon
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.bridgeToMutable
import com.eimsound.eimusic.viewmodel.AuthViewModel
import com.jakewharton.mosaic.LocalTerminalState
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

    // 窄窗口下逐级隐藏次要控件，保证曲目区域仍有空间，避免左右重叠。
    val columns = LocalTerminalState.current.size.columns
    var showTheme = columns >= 64
    var showMode = columns >= 72
    var showVolume = columns >= 82
    var showUserLabel = columns >= 92
    var showChannel = channelLabel != null && columns >= 110

    fun reservedWidth(): Int {
        var width = title.length + 1 + 1 + 3 + 1 // title + spacer + play + spacer
        if (showTheme) width += 2 + 3
        if (showMode) width += 1 + 3
        if (showVolume) width += 1 + 8
        width += 2 + (4 + queue.length) // queue
        width += 2 + 3 // search
        width += 2 + 3 // settings
        width += 2 + (3 + if (showUserLabel) stringCells(authLabel) else 0) // user
        if (showChannel) width += 2 + 3 + stringCells(channelLabel!!.take(24))
        return width
    }

    var trackWidth = columns - reservedWidth()
    while (trackWidth < 6) {
        when {
            showChannel -> showChannel = false
            showUserLabel -> showUserLabel = false
            showVolume -> showVolume = false
            showMode -> showMode = false
            showTheme -> showTheme = false
            else -> break
        }
        trackWidth = columns - reservedWidth()
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(palette.headerBg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(" $title", color = palette.titleFg)
        Spacer(Modifier.width(1))
        HeaderButton(globalActions.onPlayPause) {
            Text(controls.playIcon, color = palette.playBtnFg)
        }
        Spacer(Modifier.width(1))
        if (trackWidth > 0) {
            Text(
                fitCells(trackText, trackWidth),
                color = palette.textDimFg,
                softWrap = false,
                maxLines = 1,
            )
        }
        if (showTheme) {
            Spacer(Modifier.width(2))
            HeaderButton(onToggleTheme) {
                Text(themeIcon, color = palette.modeFg)
            }
        }
        if (showMode) {
            Spacer(Modifier.width(1))
            HeaderButton(globalActions.onTogglePlayMode) {
                Text(controls.modeIcon, color = palette.modeFg)
            }
        }
        if (showVolume) {
            Spacer(Modifier.width(1))
            HeaderButton(
                onClick = globalActions.onToggleMute,
                onWheel = { up -> globalActions.onVolumeScroll(if (up) 1 else -1) },
            ) {
                Text(controls.volIcon, color = palette.textFg)
                Spacer(Modifier.width(1))
                Text(controls.volPercent, color = palette.textDimFg)
            }
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
            if (showUserLabel) {
                Spacer(Modifier.width(1))
                Text(authLabel, color = palette.highlightFg)
            }
        }
        if (showChannel) {
            Spacer(Modifier.width(2))
            HeaderButton(onOpenTogether) {
                Text(Icon.Together.sym, color = palette.highlightFg)
                Spacer(Modifier.width(1))
                Text(channelLabel!!.take(24), color = palette.highlightFg)
            }
        }
    }
}

/** CJK / 全角字符按 2 格计算。 */
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

/** 按终端单元格截断到 [width] 格，超出时用省略号；不足则补空格。 */
private fun fitCells(text: String, width: Int): String {
    if (width <= 0) return ""
    val total = stringCells(text)
    if (total <= width) return text + " ".repeat(width - total)

    val sb = StringBuilder()
    var cells = 0
    for (c in text) {
        val w = charCells(c)
        if (cells + w > width - 1) break
        sb.append(c)
        cells += w
    }
    sb.append('…')
    cells++
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

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
import com.jakewharton.mosaic.ui.TextOverflow

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

    // 窄窗口下按列数隐藏次要控件；曲目区域由 weight 自适应剩余宽度。
    val columns = LocalTerminalState.current.size.columns
    val showTheme = columns >= 64
    val showMode = columns >= 72
    val showVolume = columns >= 82
    val showUserLabel = columns >= 92
    val showChannel = channelLabel != null && columns >= 110

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
        Text(
            trackText,
            modifier = Modifier.weight(1f),
            color = palette.textDimFg,
            softWrap = true,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

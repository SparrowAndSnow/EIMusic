package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.keyboard.LocalGlobalActions
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.ui.Icon
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.bridgeToMutable
import com.eimsound.eimusic.viewmodel.AuthViewModel
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.clickable
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.hoverable
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
    onOpenTogether: () -> Unit,
    onToggleTheme: () -> Unit,
    channelLabel: String? = null,
) {
    val palette = LocalTuiColors.current
    val globalActions = LocalGlobalActions.current
    val authState by authViewModel.authState.bridgeToMutable()
    val authLabel = when (val s = authState) {
        is AuthState.Authenticated -> s.user.username
        is AuthState.Loading -> "..."
        else -> "login"
    }.take(18)
    val queue = "${state.queue.size}"
    val themeIcon = if (isDark) Icon.Moon.sym else Icon.Sun.sym
    val controls = playControlData(state)

    Row(
        modifier = Modifier.fillMaxWidth().background(palette.headerBg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(" $title", color = palette.titleFg)
        Spacer(Modifier.width(2))
        HeaderButton(onToggleTheme) {
            Text(themeIcon, modifier = Modifier.width(1), color = palette.modeFg)
        }
        Spacer(Modifier.width(1))
        HeaderButton(globalActions.onTogglePlayMode) {
            Text(controls.modeIcon, modifier = Modifier.width(3), color = palette.modeFg)
        }
        Spacer(Modifier.width(1))
        HeaderButton(globalActions.onPlayPause) {
            Text(controls.playIcon, modifier = Modifier.width(1), color = palette.playBtnFg)
        }
        Spacer(Modifier.width(1))
        HeaderButton(globalActions.onToggleMute) {
            Text(controls.volIcon, modifier = Modifier.width(1), color = palette.textFg)
            Spacer(Modifier.width(1))
            Text(controls.volPercent, modifier = Modifier.width(4), color = palette.textDimFg)
        }
        Spacer(Modifier.width(2))
        HeaderButton(onOpenQueue) {
            Text(Icon.Queue.sym, modifier = Modifier.width(1), color = palette.textDimFg)
            Spacer(Modifier.width(1))
            Text(queue, color = palette.textFg)
        }
        Spacer(Modifier.width(2))
        HeaderButton(onNavigate) {
            Text(Icon.User.sym, modifier = Modifier.width(1), color = palette.textDimFg)
            Spacer(Modifier.width(1))
            Text(authLabel, color = palette.highlightFg)
        }
        if (channelLabel != null) {
            Spacer(Modifier.width(2))
            HeaderButton(onOpenTogether) {
                Text(Icon.Together.sym, modifier = Modifier.width(1), color = palette.highlightFg)
                Spacer(Modifier.width(1))
                Text(channelLabel.take(24), color = palette.highlightFg)
            }
        }
    }
}

@Composable
private fun HeaderButton(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalTuiColors.current
    var hovered by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .hoverable { hovered = it }
            .then(if (hovered) Modifier.background(palette.hoverBg) else Modifier)
            .padding(horizontal = 1),
        content = content,
    )
}

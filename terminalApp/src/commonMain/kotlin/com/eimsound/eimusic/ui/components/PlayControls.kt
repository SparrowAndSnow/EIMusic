package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eimsound.eimusic.keyboard.LocalGlobalActions
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.ui.Icon
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.clickable
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.hoverable
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Spacer
import com.jakewharton.mosaic.ui.Text

data class PlayControlData(val modeIcon: String, val playIcon: String, val volIcon: String, val volPercent: String)

fun playControlData(
    state: PlayerState,
): PlayControlData {
    val modeIcon = when (state.playMode) {
        is PlayMode.Shuffle -> Icon.Shuffle.sym
        is PlayMode.RepeatOne -> "${Icon.Repeat.sym} 1"
        is PlayMode.Loop -> Icon.Repeat.sym
    }
    val playIcon = when (state.playState) {
        PlayState.PLAYING -> Icon.Play.sym
        PlayState.PAUSED -> Icon.Pause.sym
        PlayState.STOPPED -> Icon.Stop.sym
    }
    val volIcon = when {
        state.volume <= 0f -> Icon.VolumeOff.sym
        state.volume < 0.34f -> Icon.VolumeLow.sym
        else -> Icon.VolumeHigh.sym
    }
    val volPercent = "${(state.volume * 100f + 0.5f).toInt()}%"

    return PlayControlData(modeIcon, playIcon, volIcon, volPercent)
}

/** Full player control strip used in MainScreen; each segment is clickable. */
@Composable
fun PlayerControls(state: PlayerState) {
    val palette = LocalTuiColors.current
    val globalActions = LocalGlobalActions.current
    val controls = playControlData(state)

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row {
            ControlButton(globalActions.onTogglePlayMode) {
                Text(controls.modeIcon, color = palette.modeFg)
            }
            Spacer(Modifier.width(2))
            ControlButton(globalActions.onPrevious) {
                Text(Icon.Previous.sym, color = palette.textFg)
            }
            Spacer(Modifier.width(1))
            ControlButton(globalActions.onPlayPause) {
                Text(controls.playIcon, color = palette.playBtnFg)
            }
            Spacer(Modifier.width(1))
            ControlButton(globalActions.onNext) {
                Text(Icon.Next.sym, color = palette.textFg)
            }
            Spacer(Modifier.width(2))
            ControlButton(globalActions.onToggleMute) {
                Text(controls.volIcon, color = palette.textFg)
                Spacer(Modifier.width(1))
                Text(controls.volPercent, color = palette.textDimFg)
            }
        }
    }
}

@Composable
private fun ControlButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val palette = LocalTuiColors.current
    var hovered by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .hoverable { hovered = it }
            .background(if (hovered) palette.hoverBg else Color.Unspecified)
            .padding(horizontal = 1),
    ) {
        content()
    }
}

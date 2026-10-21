package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.ui.Icon
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.ui.theme.TuiColors
import com.eimsound.eimusic.util.spacer
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.AnnotatedString
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Text

/** Shared colored segments — both [PlayerControls] and [HeaderBar] iterate this. */
fun AnnotatedString.Builder.playControlSegments(
    state: PlayerState,
    palette: TuiColors
) {
    val (modeIcon, playIcon, volIcon, volPercent) = playControlData(state)
    withStyle(SpanStyle(color = palette.modeFg)) { append(modeIcon) }
    spacer(3)
    withStyle(SpanStyle(color = palette.playBtnFg)) { append(playIcon) }
    spacer(2)
    withStyle(SpanStyle(color = palette.textFg)) { append(volIcon) }
    spacer(1)
    withStyle(SpanStyle(color = palette.textDimFg)) { append(volPercent) }
}

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

/** Full player control strip used in MainScreen. */
@Composable
fun PlayerControls(state: PlayerState) {
    val palette = LocalTuiColors.current

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            buildAnnotatedString {
                playControlSegments(state, palette)
            },
            color = palette.textFg,
        )
    }
}

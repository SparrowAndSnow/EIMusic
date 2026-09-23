package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.spacer
import com.eimsound.eimusic.util.truncated
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Text

@Composable
fun StatusBar(
    mode: StatusBarMode = StatusBarMode.Main,
    playerState: PlayerState? = null,
) {
    val palette = LocalTuiColors.current
    val term = LocalTerminalState.current
    val hints by LocalStatusBarHint.current
    val tag = statusBarTag(mode)

    val full = if (tag != null) "$hints${2.spacer}$tag" else hints
    val (visible, lineWidth) = full.truncated(term.size.columns)

    val leftLen = hints.length.coerceAtMost(visible.length)
    val right = if (tag != null && visible.length > leftLen) {
        visible.drop(leftLen + 2)
    } else null

    Box(modifier = Modifier.fillMaxWidth().background(palette.statusBarBg)) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = palette.statusBarFg)) {
                    append(visible.take(leftLen))
                }
                if (right != null) {
                    spacer(2)
                    withStyle(SpanStyle(color = palette.highlightFg)) {
                        append(right)
                    }
                }
            },
            Modifier.width(lineWidth),
            color = palette.statusBarFg,
        )
    }
}

enum class StatusBarMode {
    Main, Search, Login, LogViewer, Help, Playlists, LocalSongs, Together, Queue
}

private fun statusBarTag(mode: StatusBarMode): String? = when (mode) {
    StatusBarMode.Main -> null
    StatusBarMode.Search -> "SEARCH"
    StatusBarMode.Login -> "LOGIN"
    StatusBarMode.LogViewer -> "LOGS"
    StatusBarMode.Help -> "HELP"
    StatusBarMode.Playlists -> "PLAYLISTS"
    StatusBarMode.LocalSongs -> "LOCAL"
    StatusBarMode.Together -> "TOGETHER"
    StatusBarMode.Queue -> "QUEUE"
}

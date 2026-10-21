package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.spacer
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextOverflow

@Composable
fun StatusBar(
    mode: StatusBarMode = StatusBarMode.Main,
    playerState: PlayerState? = null,
) {
    val palette = LocalTuiColors.current
    val hints by LocalStatusBarHint.current
    val tag = statusBarTag(mode)

    Box(modifier = Modifier.fillMaxWidth().background(palette.statusBarBg)) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = palette.statusBarFg)) { append(hints) }
                if (tag != null) {
                    spacer(2)
                    withStyle(SpanStyle(color = palette.highlightFg)) { append(tag) }
                }
            },
            // 由 Text 在终端宽度内做单行省略，替代手算 visible/lineWidth。
            Modifier.fillMaxWidth(),
            color = palette.statusBarFg,
            softWrap = true,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

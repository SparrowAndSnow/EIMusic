package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.LocalGlobalActions
import com.eimsound.eimusic.keyboard.dispatchMainKey
import com.eimsound.eimusic.keyboard.mainStatusBarHint
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.util.bridgeToMutable
import com.eimsound.eimusic.ui.components.AlbumArt
import com.eimsound.eimusic.ui.components.LocalStatusBarHint
import com.eimsound.eimusic.ui.components.PlayerControls
import com.eimsound.eimusic.ui.components.ProgressBar
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.ui.uiKeyEvent
import com.eimsound.eimusic.util.render
import com.eimsound.eimusic.util.spacer
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.*
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.*

@Composable
fun MainScreen(
    playerState: PlayerState,
    playerViewModel: PlayerViewModel,
    onOpenHelp: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenPlaylists: () -> Unit = {},
    onOpenLocalSongs: () -> Unit = {},
    onOpenTogether: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onToggleTheme: () -> Unit,
    onQuit: () -> Unit,
) {
    val displayPos by playerViewModel.displayPosition.bridgeToMutable()
    val globalActions = LocalGlobalActions.current
    val statusBarHint = LocalStatusBarHint.current
    val uiActive = LocalUiInputEnabled.current
    LaunchedEffect(uiActive) {
        statusBarHint.value = mainStatusBarHint()
    }
    Box(
        modifier = Modifier.fillMaxSize().uiKeyEvent { event ->
            dispatchMainKey(
                event = event,
                globalActions = globalActions,
                onToggleHelp = onOpenHelp,
                onOpenQueue = onOpenQueue,
                onOpenSearch = onOpenSearch,
                onOpenLogin = onOpenLogin,
                onOpenLogs = onOpenLogs,
                onOpenPlaylists = onOpenPlaylists,
                onOpenLocalSongs = onOpenLocalSongs,
                onOpenTogether = onOpenTogether,
                onToggleTheme = onToggleTheme,
                onQuit = onQuit,
            )
        }
    ) {
        PlayerCenter(playerState, displayPos)
    }
}

@Composable
private fun PlayerCenter(playerState: PlayerState, displayPos: Long) {
    val palette = LocalTuiColors.current
    val termWidth = LocalTerminalState.current.size.columns
    val termHeight = LocalTerminalState.current.size.rows
    val contentWidth = (termWidth * 0.7f).toInt().coerceIn(30, termWidth - 4)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (playerState.playState == PlayState.STOPPED && playerState.current == null) {
                Spacer(Modifier.height(4))
                Text("♪", color = palette.textDimFg)
                Text("No track playing", color = palette.textDimFg)
                Spacer(Modifier.height(2))
                Text("Press 'f' to search  •  'o' for queue", color = palette.textDimFg)
            } else {
                // ── Album art (roughly square considering font 1:2 ratio) ──
                val artWidth = contentWidth.coerceAtMost(40)
                val artHeight = (artWidth / 2).coerceIn(6, (termHeight - 12).coerceAtLeast(6))
                Box(
                    modifier = Modifier
                        .width(artWidth)
                        .height(artHeight)
                        .align(Alignment.CenterHorizontally)
                ) {
                    AlbumArt(
                        imageUrl = playerState.current?.album?.image,
                        width = artWidth,
                        height = artHeight,
                    )
                }

                Spacer(Modifier.height(2))

                // ── Title & Artist ──
                val title = playerState.current?.name
                    ?.ifBlank { playerState.current?.id?.toString() ?: "Unknown" }
                    ?: "Unknown"
                val artist = playerState.current?.artists?.firstOrNull()?.name
                    ?.ifBlank { "Unknown Artist" } ?: "Unknown Artist"

                Text(
                    buildAnnotatedString {
                        spacer(2)
                        withStyle(SpanStyle(color = palette.textFg)) { append(title.render()) }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = palette.textFg,
                )
                Text(
                    buildAnnotatedString {
                        spacer(2)
                        withStyle(SpanStyle(color = palette.textDimFg)) { append(artist.render()) }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = palette.textDimFg,
                )

                Spacer(Modifier.height(2))

                // ── Progress bar ──
                val barWidth = contentWidth.coerceIn(20, 50)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ProgressBar(
                        current = displayPos,
                        total = playerState.duration,
                        width = barWidth,
                    )
                }

                if (playerState.isLoading) {
                    Text(
                        "  Loading...  ",
                        color = palette.highlightFg,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }

                Spacer(Modifier.height(1))

                // ── Controls ──
                PlayerControls(playerState)
            }
        }
    }
}

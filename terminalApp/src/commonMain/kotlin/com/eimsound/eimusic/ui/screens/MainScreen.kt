package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.LocalGlobalActions
import com.eimsound.eimusic.keyboard.dispatchMainKey
import com.eimsound.eimusic.keyboard.mainStatusBarHint
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.util.bridgeToMutable
import com.eimsound.eimusic.ui.components.AlbumArt
import com.eimsound.eimusic.ui.components.ListView
import com.eimsound.eimusic.ui.components.PlayerControls
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.components.ProgressBar
import com.eimsound.eimusic.ui.theme.LocalTuiColors
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
    onOpenSettings: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenLogs: () -> Unit,
    onToggleTheme: () -> Unit,
    onQuit: () -> Unit,
) {
    val displayPos by playerViewModel.displayPosition.bridgeToMutable()
    val recentTracks by playerViewModel.recentTracks.collectAsState()
    val globalActions = LocalGlobalActions.current
    ScreenFrame(
        hint = mainStatusBarHint(),
        onKey = { event ->
            dispatchMainKey(
                event = event,
                globalActions = globalActions,
                onToggleHelp = onOpenHelp,
                onOpenSearch = onOpenSearch,
                onOpenSettings = onOpenSettings,
                onOpenLogin = onOpenLogin,
                onOpenLogs = onOpenLogs,
                onToggleTheme = onToggleTheme,
                onQuit = onQuit,
            )
        },
    ) {
        PlayerCenter(
            playerState = playerState,
            displayPos = displayPos,
            onSeek = playerViewModel::seek,
            recentTracks = recentTracks,
            onPlayRecent = { playerViewModel.playTrackFromList(recentTracks, it) },
        )
    }
}

@Composable
private fun PlayerCenter(
    playerState: PlayerState,
    displayPos: Long,
    onSeek: (Long) -> Unit,
    recentTracks: List<Track>,
    onPlayRecent: (Track) -> Unit,
) {
    val palette = LocalTuiColors.current
    val termWidth = LocalTerminalState.current.size.columns
    val termHeight = LocalTerminalState.current.size.rows
    val contentWidth = (termWidth * 0.7f).toInt().coerceIn(30, termWidth - 4)
    val recentWidth = (termWidth * 0.6f).toInt().coerceIn(24, 60)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (playerState.playState == PlayState.STOPPED && playerState.current == null) {
                Spacer(Modifier.height(4))
                Text(
                    "♪",
                    modifier = Modifier.fillMaxWidth(),
                    color = palette.textDimFg,
                    textAlign = Alignment.CenterHorizontally,
                )
                Text(
                    "No track playing",
                    modifier = Modifier.fillMaxWidth(),
                    color = palette.textDimFg,
                    textAlign = Alignment.CenterHorizontally,
                )
                Spacer(Modifier.height(2))
                if (recentTracks.isEmpty()) {
                    Text(
                        "Press 'f' to search  •  1-5 switch tabs",
                        modifier = Modifier.fillMaxWidth(),
                        color = palette.textDimFg,
                        textAlign = Alignment.CenterHorizontally,
                    )
                } else {
                    Box(Modifier.width(recentWidth).align(Alignment.CenterHorizontally)) {
                        Column {
                            Text(
                                "Recent",
                                modifier = Modifier.fillMaxWidth(),
                                color = palette.titleFg,
                                textAlign = Alignment.CenterHorizontally,
                            )
                            ListView(
                                items = recentTracks,
                                visibleLines = 6,
                                emptyMessage = "",
                                onConfirm = onPlayRecent,
                                render = { track, _, selected ->
                                    val marker = if (selected) "▸" else " "
                                    val artist = track.artists?.firstOrNull()?.name?.render().orEmpty()
                                    Text(
                                        " $marker ${track.name.render()}  —  $artist",
                                        modifier = Modifier.fillMaxWidth(),
                                        color = if (selected) palette.highlightFg else palette.textDimFg,
                                        textAlign = Alignment.CenterHorizontally,
                                        softWrap = false,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }
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
                ProgressBar(
                    current = displayPos,
                    total = playerState.duration,
                    width = barWidth,
                    onSeek = onSeek,
                )

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

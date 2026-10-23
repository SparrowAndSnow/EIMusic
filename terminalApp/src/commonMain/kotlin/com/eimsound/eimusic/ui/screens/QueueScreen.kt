package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.QueueKeyActions
import com.eimsound.eimusic.keyboard.dispatchQueueKey
import com.eimsound.eimusic.keyboard.queueStatusBarHint
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.ui.components.ContextMenuItem
import com.eimsound.eimusic.ui.components.LocalTrackActionHost
import com.eimsound.eimusic.ui.components.PlaylistPicker
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.components.TrackTable
import com.eimsound.eimusic.ui.components.rememberListSelectionState
import com.eimsound.eimusic.ui.components.rememberPlaylistPickerState
import com.eimsound.eimusic.ui.components.trackContextItems
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.viewmodel.PlaylistViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.jakewharton.mosaic.layout.*
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.*

@Composable
fun QueueScreen(
    playerState: PlayerState,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val safeArea = LocalSafeContentSize.current
    val termHeight = safeArea.rows
    val termWidth = safeArea.columns
    val visibleLines = (termHeight - 4).coerceAtLeast(1)
    val selection = rememberListSelectionState()
    val playlistState by playlistViewModel.state.collectAsState()
    val localPlaylists = playlistState.playlists.filter { it.id.source == TrackSource.LOCAL }
    val picker = rememberPlaylistPickerState()
    val trackActions = LocalTrackActionHost.current

    ScreenFrame(
        hint = queueStatusBarHint(),
        onKey = { event ->
            if (picker.isOpen) {
                if (event.key == "Escape") {
                    picker.close()
                    true
                } else {
                    false // picker ListView handles j/k/Enter
                }
            } else if (event.key == "l" && !event.ctrl && !event.alt) {
                playerState.queue.getOrNull(selection.index)?.let(picker::open)
                true
            } else {
                dispatchQueueKey(
                    event = event,
                    actions = QueueKeyActions(
                        moveUp = {
                            val index = selection.index
                            if (index > 0 && index < playerState.queue.size) {
                                playerViewModel.moveTrack(index, index - 1)
                                selection.jumpTo(index - 1)
                            }
                        },
                        moveDown = {
                            val index = selection.index
                            if (index >= 0 && index < playerState.queue.size - 1) {
                                playerViewModel.moveTrack(index, index + 1)
                                selection.jumpTo(index + 1)
                            }
                        },
                        jumpToCurrent = {
                            val index = playerState.index
                            if (index in playerState.queue.indices) selection.jumpTo(index)
                        },
                        removeTrack = {
                            val index = selection.index
                            if (index >= 0 && index < playerState.queue.size) {
                                playerViewModel.removeTrack(index)
                                selection.jumpTo((index - 1).coerceAtLeast(0))
                            }
                        },
                        clearQueue = {
                            playerViewModel.clearQueue()
                            selection.jumpTo(0)
                        },
                        goBack = onGoBack,
                    ),
                )
            }
        },
    ) {
        Column {
            Text(" Queue", color = palette.titleFg)

            if (picker.isOpen) {
                PlaylistPicker(
                    state = picker,
                    playlists = localPlaylists,
                    visibleLines = (visibleLines - 2).coerceAtLeast(1),
                ) { playlist, track ->
                    playlistViewModel.addTrackToPlaylist(playlist.id.id, track)
                }
            } else {
                TrackTable(
                    tracks = playerState.queue,
                    currentTrackId = playerState.current?.id,
                    termWidth = termWidth,
                    visibleLines = visibleLines,
                    onPlayTrack = { track -> playerViewModel.playTrack(track) },
                    selection = selection,
                    onSecondaryClick = { track, index ->
                        trackActions?.showMenu(
                            track.name,
                            trackContextItems(
                                onAddToPlaylist = { picker.open(track) },
                                onPlayNext = { playerViewModel.playNext(track) },
                                onShowInfo = { trackActions?.showInfo(track) },
                                extra = listOf(
                                    ContextMenuItem("Remove from queue") {
                                        playerViewModel.removeTrack(index)
                                        selection.jumpTo((index - 1).coerceAtLeast(0))
                                    },
                                ),
                            ),
                        )
                    },
                    onConfirm = {
                        playerViewModel.playTrack(it)
                        onGoBack()
                    }
                )
            }
        }
    }
}


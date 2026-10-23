package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.QueueKeyActions
import com.eimsound.eimusic.keyboard.dispatchQueueKey
import com.eimsound.eimusic.keyboard.queueStatusBarHint
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.ui.components.ListView
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.components.TrackTable
import com.eimsound.eimusic.ui.components.rememberListSelectionState
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
    var pickerTrack by remember { mutableStateOf<Track?>(null) }
    val pickerSelection = rememberListSelectionState()

    ScreenFrame(
        hint = queueStatusBarHint(),
        onKey = { event ->
            val picking = pickerTrack
            if (picking != null) {
                if (event.key == "Escape") {
                    pickerTrack = null
                    true
                } else {
                    false // picker ListView handles j/k/Enter
                }
            } else if (event.key == "l" && !event.ctrl && !event.alt) {
                pickerTrack = playerState.queue.getOrNull(selection.index)
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

            val track = pickerTrack
            if (track != null) {
                Text("  Add to local playlist", color = palette.titleFg)
                ListView(
                    items = localPlaylists,
                    visibleLines = (visibleLines - 2).coerceAtLeast(1),
                    emptyMessage = "No local playlists. Press n on Playlists to create one.",
                    selection = pickerSelection,
                    onConfirm = { playlist ->
                        playlistViewModel.addTrackToPlaylist(playlist.id.id, track)
                        pickerTrack = null
                    },
                    render = { playlist, _, selected ->
                        val marker = if (selected) "▸" else " "
                        Text(
                            " $marker " + playlist.displayName(),
                            color = if (selected) palette.highlightFg else palette.textDimFg,
                        )
                    },
                )
            } else {
                TrackTable(
                    tracks = playerState.queue,
                    currentTrackId = playerState.current?.id,
                    termWidth = termWidth,
                    visibleLines = visibleLines,
                    onPlayTrack = { track -> playerViewModel.playTrack(track) },
                    selection = selection,
                    onConfirm = {
                        playerViewModel.playTrack(it)
                        onGoBack()
                    }
                )
            }
        }
    }
}

private fun MediaList.displayName(): String = when (this) {
    is MediaList.PlaylistMediaList -> playlist.name
    is MediaList.AlbumMediaList -> album.name
}

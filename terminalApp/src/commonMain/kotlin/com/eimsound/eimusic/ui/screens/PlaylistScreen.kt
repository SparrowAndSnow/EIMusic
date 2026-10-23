package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.PlaylistFocus
import com.eimsound.eimusic.keyboard.PlaylistKeyActions
import com.eimsound.eimusic.keyboard.dispatchPlaylistKey
import com.eimsound.eimusic.keyboard.playlistStatusBarHint
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.ui.components.InputField
import com.eimsound.eimusic.ui.components.ListView
import com.eimsound.eimusic.ui.components.rememberListSelectionState
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.components.TrackTable
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.spacer
import com.eimsound.eimusic.viewmodel.PlaylistViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.jakewharton.mosaic.layout.*
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.*

@Composable
fun PlaylistScreen(
    playlistViewModel: PlaylistViewModel,
    playerViewModel: PlayerViewModel,
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val safeArea = LocalSafeContentSize.current
    val state = playlistViewModel.state.collectAsState().value
    val termWidth = safeArea.columns
    val termHeight = safeArea.rows
    val visibleLines = (termHeight - 4).coerceAtLeast(1)

    var focus by remember { mutableStateOf(PlaylistFocus.List) }
    var playlistIndex by remember { mutableIntStateOf(0) }
    val trackSelection = rememberListSelectionState()
    var showTrackList by remember { mutableStateOf(false) }
    var creatingLocal by remember { mutableStateOf(false) }

    ScreenFrame(
        hint = playlistStatusBarHint(focus),
        onKey = { event ->
            dispatchPlaylistKey(
                event = event,
                focus = focus,
                showTrackList = showTrackList,
                actions = PlaylistKeyActions(
                    setShowTrackList = { showTrackList = it },
                    setFocus = { focus = it },
                    goBack = onGoBack,
                    beginImport = {
                        creatingLocal = false
                        playlistViewModel.updateAddUrl("")
                        focus = PlaylistFocus.Input
                    },
                    beginLocal = {
                        creatingLocal = true
                        playlistViewModel.updateAddUrl("")
                        focus = PlaylistFocus.Input
                    },
                    removePlaylist = {
                        state.playlists.getOrNull(playlistIndex)?.let { playlistViewModel.removePlaylist(it) }
                        playlistIndex = (playlistIndex - 1).coerceAtLeast(0)
                    },
                    selectPlaylist = {
                        state.playlists.getOrNull(playlistIndex)?.let { p ->
                            playlistViewModel.selectPlaylist(p)
                        }
                    },
                    removeTrack = {
                        if (state.selectedPlaylist?.id?.source == com.eimsound.eimusic.music.TrackSource.LOCAL) {
                            val index = trackSelection.index
                            playlistViewModel.removeTrackAt(index)
                            trackSelection.jumpTo((index - 1).coerceAtLeast(0))
                        }
                    },
                    moveTrackUp = {
                        if (state.selectedPlaylist?.id?.source == com.eimsound.eimusic.music.TrackSource.LOCAL) {
                            val index = trackSelection.index
                            if (index > 0) {
                                playlistViewModel.moveTrack(index, index - 1)
                                trackSelection.jumpTo(index - 1)
                            }
                        }
                    },
                    moveTrackDown = {
                        if (state.selectedPlaylist?.id?.source == com.eimsound.eimusic.music.TrackSource.LOCAL) {
                            val index = trackSelection.index
                            if (index < state.tracks.lastIndex) {
                                playlistViewModel.moveTrack(index, index + 1)
                                trackSelection.jumpTo(index + 1)
                            }
                        }
                    },
                ),
            )
        },
    ) {
        when (focus) {
            PlaylistFocus.Input -> InputView(
                value = state.addUrlInput,
                title = if (creatingLocal) "Enter local playlist name:" else "Enter playlist URL:",
                placeholder = if (creatingLocal) "My playlist" else "https://music.163.com/playlist/...",
                onValueChange = { playlistViewModel.updateAddUrl(it) },
                onConfirm = {
                    val text = state.addUrlInput.trim()
                    if (text.isNotBlank()) {
                        if (creatingLocal) {
                            playlistViewModel.createLocalPlaylist(text)
                        } else {
                            playlistViewModel.addPlaylist(text)
                        }
                        playlistViewModel.updateAddUrl("")
                    }
                    creatingLocal = false
                    focus = PlaylistFocus.List
                },
                onCancel = {
                    creatingLocal = false
                    playlistViewModel.updateAddUrl("")
                    focus = PlaylistFocus.List
                },
            )

            else -> Column {
                // Title bar
                val title = when (focus) {
                    PlaylistFocus.TrackList -> "Playlist Tracks"
                    else -> "My Playlists"
                }
                Text(" $title", color = palette.titleFg)
                state.error?.let { error ->
                    Text("  Error: $error", color = palette.errorFg)
                }

                if (focus == PlaylistFocus.TrackList) {
                    TrackTable(
                        tracks = state.tracks,
                        currentTrackId = playerViewModel.state.value.current?.id,
                        termWidth = termWidth,
                        visibleLines = visibleLines,
                        emptyMessage = "No tracks in this playlist.",
                        selection = trackSelection,
                        onPlayTrack = {
                            playerViewModel.playTrack(it)
                            onGoBack()
                        },
                        onPlayAll = {
                            if (state.tracks.isNotEmpty()) {
                                playerViewModel.playAll(state.tracks)
                                onGoBack()
                            }
                        },
                        onConfirm = {
                            playerViewModel.playTrack(it)
                            onGoBack()
                        },
                        onAddNext = {
                        },
                        onAddLast = {
                        }
                    )
                } else {
                    PlaylistListView(
                        playlists = state.playlists,
                        termHeight = termHeight,
                        termWidth = termWidth,
                        isLoading = state.isLoading,
                        onSelectedChange = { playlistIndex = it },
                        onConfirm = {
                            playlistViewModel.selectPlaylist(it)
                            focus = PlaylistFocus.TrackList
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InputView(
    value: String,
    title: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val palette = LocalTuiColors.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text(" $title", color = palette.titleFg)
            Spacer(Modifier.height(1))
            Box(modifier = Modifier.border(color = palette.dimBorderFg)) {
                InputField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = placeholder,
                    onConfirm = onConfirm,
                    onCancel = onCancel,
                )
            }
            Spacer(Modifier.height(1))
            Text(" Enter to confirm  ·  Esc to cancel", color = palette.textDimFg)
        }
    }
}

@Composable
private fun PlaylistListView(
    playlists: List<MediaList>,
    termHeight: Int,
    termWidth: Int,
    isLoading: Boolean,
    onSelectedChange: (Int) -> Unit = {},
    onConfirm: (MediaList) -> Unit = {},
) {
    val palette = LocalTuiColors.current
    val visibleLines = (termHeight - 4).coerceAtLeast(1)
    val nameWidth = (termWidth / 3).coerceIn(15, 40)

    if (playlists.isEmpty() && !isLoading) {
        Spacer(Modifier.height(2))
        Text(" No playlists saved.", color = palette.textDimFg)
        Text(" Press 'a' to add a playlist URL.", color = palette.textDimFg)
        return
    }

    if (isLoading) Text(" Loading...", color = palette.textDimFg)

    Row {
        Spacer(Modifier.width(2))
        Box(Modifier.width(nameWidth)) {
            Text(
                "Name",
                Modifier.fillMaxWidth(),
                color = palette.textDimFg,
                softWrap = true,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text("   Tracks", color = palette.textDimFg)
    }

    ListView(
        items = playlists,
        visibleLines = visibleLines,
        emptyMessage = "",
        onSelectedChange = onSelectedChange,
        onConfirm = onConfirm,
        render = { playlist, i, isSelected ->
            val name = when (playlist) {
                is MediaList.PlaylistMediaList -> playlist.playlist.name
                is MediaList.AlbumMediaList -> playlist.album.name
            }
            val trackCount = when (playlist) {
                is MediaList.PlaylistMediaList -> "${playlist.playlist.tracks.size}"
                is MediaList.AlbumMediaList -> "${playlist.album.tracks.size}"
            }
            val rowModifier = if (isSelected) {
                Modifier.fillMaxWidth().background(palette.selectionBg)
            } else {
                Modifier.fillMaxWidth()
            }
            Row(rowModifier) {
                Text(
                    if (isSelected) "▶" else " ",
                    color = if (isSelected) palette.currentMarkerFg else palette.textDimFg,
                )
                Spacer(Modifier.width(1))
                Box(Modifier.width(nameWidth)) {
                    Text(
                        name,
                        Modifier.fillMaxWidth(),
                        color = if (isSelected) palette.highlightFg else palette.textFg,
                        softWrap = true,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("  ", color = palette.textDimFg)
                Text(trackCount.padStart(4), color = palette.textDimFg)
                Text(" tracks", color = palette.textDimFg)
            }
        },
    )
}


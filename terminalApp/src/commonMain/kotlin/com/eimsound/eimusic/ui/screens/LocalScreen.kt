package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.LocalFocus
import com.eimsound.eimusic.keyboard.LocalSongsActions
import com.eimsound.eimusic.keyboard.dispatchLocalSongsKey
import com.eimsound.eimusic.keyboard.localSongsStatusBarHint
import com.jakewharton.mosaic.layout.border
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.ui.components.InputField
import com.eimsound.eimusic.ui.components.LocalTrackActionHost
import com.eimsound.eimusic.ui.components.PlaylistPicker
import com.eimsound.eimusic.ui.components.ListView
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.components.TrackTable
import com.eimsound.eimusic.ui.components.rememberListSelectionState
import com.eimsound.eimusic.ui.components.rememberPlaylistPickerState
import com.eimsound.eimusic.ui.components.trackContextItems
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.spacer
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.eimsound.eimusic.viewmodel.PlaylistViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.jakewharton.mosaic.layout.*
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.*

@Composable
fun LocalScreen(
    localViewModel: LocalViewModel,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val safeArea = LocalSafeContentSize.current
    val state = localViewModel.state.collectAsState().value
    val termHeight = safeArea.rows
    val termWidth = safeArea.columns
    var focus by remember { mutableStateOf(LocalFocus.Songs) }
    val trackSelection = rememberListSelectionState()
    val picker = rememberPlaylistPickerState()
    val trackActions = LocalTrackActionHost.current
    val showTrackMenu: (Track, Int) -> Unit = { track, _ ->
        trackActions?.showMenu(
            track.name,
            trackContextItems(
                onAddToPlaylist = { picker.open(track) },
                onPlayNext = { playerViewModel.playNext(track) },
                onShowInfo = { trackActions?.showInfo(track) },
            ),
        )
    }
    var pathIndex by remember { mutableIntStateOf(0) }
    var showPaths by remember { mutableStateOf(false) }

    ScreenFrame(
        hint = localSongsStatusBarHint(focus),
        onKey = { event ->
            dispatchLocalSongsKey(
                event = event,
                focus = focus,
                showPaths = showPaths,
                actions = LocalSongsActions(
                    setShowPaths = { showPaths = it },
                    setFocus = { focus = it },
                    goBack = onGoBack,
                    addPath = {
                        if (state.pathInput.isNotBlank()) {
                            localViewModel.addPath(state.pathInput.trim())
                            localViewModel.updatePathInput("")
                        }
                    },
                    removePath = {
                        state.localPaths.toList().getOrNull(pathIndex)?.let { localViewModel.removePath(it) }
                        pathIndex = (pathIndex - 1).coerceAtLeast(0)
                    },
                    refresh = { localViewModel.refreshTracks() },
                    openMenu = {
                        state.tracks.getOrNull(trackSelection.index)?.let { showTrackMenu(it, trackSelection.index) }
                    },
                ),
            )
        },
    ) {
        Column {
            val title = when {
                focus == LocalFocus.Input -> "Add Path"
                showPaths -> "Local Paths (${state.localPaths.size})"
                else -> "Local Songs (${state.tracks.size})"
            }
            Text(" $title", color = palette.titleFg)

            when (focus) {
                LocalFocus.Input -> PathInputView(
                    value = state.pathInput,
                    onValueChange = { localViewModel.updatePathInput(it) },
                    onConfirm = {
                        if (state.pathInput.isNotBlank()) {
                            localViewModel.addPath(state.pathInput.trim())
                            localViewModel.updatePathInput("")
                        }
                        focus = LocalFocus.Songs
                    },
                    onCancel = { focus = LocalFocus.Songs },
                )
                LocalFocus.Paths -> PathListView(
                    paths = state.localPaths.toList(),
                    termHeight = termHeight,
                    onSelectedChange = { pathIndex = it },
                )
                LocalFocus.Songs -> {
                    if (picker.isOpen) {
                        PlaylistPicker(
                            state = picker,
                            playlists = playlistViewModel.state.collectAsState().value.playlists
                                .filter { it.id.source == TrackSource.LOCAL },
                            visibleLines = (termHeight - 4).coerceAtLeast(1),
                        ) { playlist, track ->
                            playlistViewModel.addTrackToPlaylist(playlist.id.id, track)
                        }
                    } else {
                        if (state.isLoading) {
                            Text(" Scanning...", color = palette.textDimFg)
                        }
                        if (state.tracks.isEmpty() && !state.isLoading) {
                            Spacer(Modifier.height(2))
                            Text(" No local songs found.", color = palette.textDimFg)
                            Text(" Press '/' to manage paths.", color = palette.textDimFg)
                        }
                        TrackTable(
                            tracks = state.tracks,
                            currentTrackId = playerViewModel.state.value.current?.id,
                            termWidth = termWidth,
                            visibleLines = (termHeight - 2).coerceAtLeast(1),
                            onPlayTrack = {
                                playerViewModel.playTrackFromList(state.tracks, it)
                                onGoBack()
                            },
                            selection = trackSelection,
                            onSecondaryClick = showTrackMenu,
                            onConfirm = { track ->
                                playerViewModel.playTrackFromList(state.tracks, track)
                                onGoBack()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PathInputView(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val palette = LocalTuiColors.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text(" Enter directory path:", color = palette.titleFg)
            Spacer(Modifier.height(1))
            Box(modifier = Modifier.border(color = palette.dimBorderFg)) {
                InputField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = "/home/user/Music",
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
private fun PathListView(
    paths: List<String>,
    termHeight: Int,
    onSelectedChange: (Int) -> Unit = {},
) {
    val palette = LocalTuiColors.current
    val visibleLines = (termHeight - 2).coerceAtLeast(1)

    if (paths.isEmpty()) {
        Spacer(Modifier.height(2))
        Text(" No paths configured.", color = palette.textDimFg)
        Text(" Press 'a' to add a music directory.", color = palette.textDimFg)
        return
    }

    Text(buildAnnotatedString {
        spacer(1)
        withStyle(SpanStyle(color = palette.textDimFg)) { append("Path") }
    })

    ListView(
        items = paths,
        visibleLines = visibleLines,
        onSelectedChange = onSelectedChange,
        render = { path, i, isSelected ->
            Box(modifier = if (isSelected) Modifier.fillMaxWidth().background(palette.selectionBg) else Modifier.fillMaxWidth()) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = if (isSelected) palette.currentMarkerFg else palette.textDimFg)) { append(if (isSelected) "▶" else " ") }
                    spacer(1)
                    withStyle(SpanStyle(color = palette.titleFg)) { append("♫") }
                    spacer(1)
                    withStyle(SpanStyle(color = if (isSelected) palette.highlightFg else palette.textFg)) { append(path) }
                })
            }
        },
    )
}

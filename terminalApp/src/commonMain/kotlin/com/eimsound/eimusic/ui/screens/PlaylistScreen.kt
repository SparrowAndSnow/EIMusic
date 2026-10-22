package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.PlaylistFocus
import com.eimsound.eimusic.keyboard.dispatchPlaylistKey
import com.eimsound.eimusic.keyboard.playlistStatusBarHint
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.ui.components.InputField
import com.eimsound.eimusic.ui.components.ListView
import com.eimsound.eimusic.ui.components.LocalStatusBarHint
import com.eimsound.eimusic.ui.components.TrackTable
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.ui.uiKeyEvent
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
    val statusBarHint = LocalStatusBarHint.current
    val visibleLines = (termHeight - 4).coerceAtLeast(1)

    var focus by remember { mutableStateOf(PlaylistFocus.List) }
    val uiActive = LocalUiInputEnabled.current
    LaunchedEffect(uiActive, focus) {
        statusBarHint.value = playlistStatusBarHint(focus)
    }
    var playlistIndex by remember { mutableIntStateOf(0) }
    var trackIndex by remember { mutableIntStateOf(0) }
    var showTrackList by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().uiKeyEvent { event ->
        dispatchPlaylistKey(
            event = event,
            focus = focus,
            showTrackList = showTrackList,
            onSetShowTrackList = { showTrackList = it },
            onSetFocus = { focus = it },
            onGoBack = onGoBack,
            onAddPlaylist = {
                if (state.addUrlInput.isNotBlank()) {
                    playlistViewModel.addPlaylist(state.addUrlInput.trim())
                    playlistViewModel.updateAddUrl("")
                }
            },
            onRemovePlaylist = {
                state.playlists.getOrNull(playlistIndex)?.let { playlistViewModel.removePlaylist(it) }
                playlistIndex = (playlistIndex - 1).coerceAtLeast(0)
            },
            onSelectPlaylist = {
                state.playlists.getOrNull(playlistIndex)?.let { p ->
                    playlistViewModel.selectPlaylist(p)
                }
            },
        )
    }) {
        when (focus) {
            PlaylistFocus.Input -> InputView(
                value = state.addUrlInput,
                onValueChange = { playlistViewModel.updateAddUrl(it) },
                onConfirm = {
                    if (state.addUrlInput.isNotBlank()) {
                        playlistViewModel.addPlaylist(state.addUrlInput.trim())
                        playlistViewModel.updateAddUrl("")
                    }
                    focus = PlaylistFocus.List
                },
                onCancel = { focus = PlaylistFocus.List },
            )

            else -> Column {
                // Title bar
                val title = when (focus) {
                    PlaylistFocus.TrackList -> "Playlist Tracks"
                    else -> "My Playlists"
                }
                Text(" $title", color = palette.titleFg)

                if (focus == PlaylistFocus.TrackList) {
                    TrackTable(
                        tracks = state.tracks,
                        currentTrackId = playerViewModel.state.value.current?.id,
                        termWidth = termWidth,
                        visibleLines = visibleLines,
                        emptyMessage = "No tracks in this playlist.",
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
                        onSelectedChange = { trackIndex = it },
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
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val palette = LocalTuiColors.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text(" Enter playlist URL:", color = palette.titleFg)
            Spacer(Modifier.height(1))
            Box(modifier = Modifier.border(color = palette.dimBorderFg)) {
                InputField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = "https://music.163.com/playlist/...",
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


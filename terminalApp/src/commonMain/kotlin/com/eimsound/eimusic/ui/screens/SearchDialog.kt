package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.SearchFocus
import com.eimsound.eimusic.keyboard.dispatchSearchKey
import com.eimsound.eimusic.keyboard.searchStatusBarHint
import com.eimsound.eimusic.ui.components.DialogPanel
import com.eimsound.eimusic.ui.components.InputField
import com.eimsound.eimusic.ui.components.ListView
import com.eimsound.eimusic.ui.components.LocalStatusBarHint
import com.eimsound.eimusic.ui.components.TrackListItem
import com.eimsound.eimusic.ui.components.rememberListSelectionState
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.bridgeToMutable
import com.eimsound.eimusic.util.render
import com.eimsound.eimusic.util.spacer
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.viewmodel.PlaylistViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.SearchViewModel
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.clickable
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Dialog as OverlayDialog
import com.jakewharton.mosaic.ui.Text

@Composable
fun SearchDialog(
    searchViewModel: SearchViewModel,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onGoBack: () -> Unit,
) {
    val searchState by searchViewModel.state.bridgeToMutable()
    var focus by remember { mutableStateOf(SearchFocus.Input) }
    val resultSelection = rememberListSelectionState()
    val pickerSelection = rememberListSelectionState()
    val playlistState by playlistViewModel.state.collectAsState()
    val localPlaylists = playlistState.playlists.filter { it.id.source == TrackSource.LOCAL }
    var pickerTrack by remember { mutableStateOf<Track?>(null) }
    val palette = LocalTuiColors.current
    val statusBarHint = LocalStatusBarHint.current
    val termHeight = LocalTerminalState.current.size.rows
    // Dialog overhead ≈ 11 rows, available = termHeight - 2, leave breathing room
    val visibleLines = ((termHeight - 14) / 2).coerceIn(3, 12)

    LaunchedEffect(focus) {
        statusBarHint.value = searchStatusBarHint(focus)
    }

    OverlayDialog(onDismissRequest = onGoBack) {
        DialogPanel(title = "Search", onDismiss = onGoBack) {
            Column(
                modifier = Modifier
                    .padding(2)
                    .onKeyEvent { event ->
                        val picking = pickerTrack
                        if (picking != null) {
                            if (event.key == "Escape") {
                                pickerTrack = null
                                true
                            } else {
                                false // picker ListView handles j/k/Enter
                            }
                        } else if (event.key == "l" && focus == SearchFocus.Results && !event.ctrl && !event.alt) {
                            pickerTrack = searchState.results.getOrNull(resultSelection.index)
                            true
                        } else {
                            dispatchSearchKey(
                                event = event,
                                focus = focus,
                                onSwitchToInput = { focus = SearchFocus.Input },
                            )
                        }
                    },
            ) {
                InputField(
                    value = searchState.query,
                    onValueChange = { searchViewModel.updateQuery(it) },
                    placeholder = "Type and press Enter to search",
                    showCursor = focus == SearchFocus.Input,
                    enabled = focus == SearchFocus.Input,
                    onConfirm = {
                        searchViewModel.submitSearch()
                        focus = SearchFocus.Results
                    },
                    onCancel = {
                        if (searchState.query.isNotEmpty()) {
                            searchViewModel.updateQuery("")
                        } else {
                            searchViewModel.reset()
                            onGoBack()
                        }
                    },
                )
                if (searchState.query.isBlank() && searchState.recentSearches.isNotEmpty()) {
                    Text("  Recent", color = palette.titleFg)
                    searchState.recentSearches.take(6).forEach { recent ->
                        Text(
                            "  $recent",
                            modifier = Modifier.clickable {
                                searchViewModel.updateQuery(recent)
                                searchViewModel.submitSearch()
                                focus = SearchFocus.Results
                            },
                            color = palette.textDimFg,
                        )
                    }
                    Text(" ", color = palette.textFg)
                }
                Text(
                    buildAnnotatedString {
                        spacer(2)
                        if (focus == SearchFocus.Input) {
                            withStyle(SpanStyle(color = palette.textDimFg)) { append("⏎ search") }
                            withStyle(SpanStyle(color = palette.textDimFg)) { append("  ⎋ cancel") }
                        } else {
                            withStyle(SpanStyle(color = palette.highlightFg)) { append("▸ select") }
                            withStyle(SpanStyle(color = palette.textDimFg)) { append("  ⏎ play") }
                            withStyle(SpanStyle(color = palette.textDimFg)) { append("  ⎋ back") }
                        }
                    },
                    color = palette.textFg,
                )

                // ── Loading / Error ──
                if (searchState.isLoading) {
                    Text("  Searching...", color = palette.textDimFg)
                }
                searchState.error?.let {
                    Text("  Error: $it", color = palette.errorFg)
                }

                // ── Add to playlist picker ──
                val adding = pickerTrack
                if (adding != null) {
                    Text("  Add to local playlist", color = palette.titleFg)
                    ListView(
                        items = localPlaylists,
                        visibleLines = visibleLines,
                        emptyMessage = "No local playlists.",
                        selection = pickerSelection,
                        onConfirm = { playlist ->
                            playlistViewModel.addTrackToPlaylist(playlist.id.id, adding)
                            pickerTrack = null
                        },
                        render = { playlist, _, isSelected ->
                            val name = when (playlist) {
                                is MediaList.PlaylistMediaList -> playlist.playlist.name
                                is MediaList.AlbumMediaList -> playlist.album.name
                            }
                            Text(
                                (if (isSelected) " ▸ " else "   ") + name,
                                color = if (isSelected) palette.highlightFg else palette.textDimFg,
                            )
                        },
                    )
                }

                // ── Results list ──
                if (adding == null && !searchState.isLoading && searchState.results.isNotEmpty()) {
                    Text(" ", color = palette.textFg)
                    val focusLabel = if (focus == SearchFocus.Results) "▸ Results" else "Results"
                    Text("  $focusLabel", color = palette.titleFg)
                    ListView(
                        items = searchState.results,
                        visibleLines = visibleLines,
                        emptyMessage = "",
                        selection = resultSelection,
                        onConfirm = { track ->
                            playerViewModel.playTrack(track)
                            searchViewModel.reset()
                            onGoBack()
                        },
                        render = { track, i, isSelected ->
                            val name = track.name.render()
                            val artist = (track.artists?.firstOrNull()?.name ?: "").render()
                            TrackListItem(
                                index = i,
                                isSelected = isSelected,
                                number = i + 1,
                                name = name,
                                artist = artist,
                                nameMax = 45,
                                artistMax = 20,
                                isCurrent = false,
                            )
                        },
                    )
                }
            }
        }
    }
}

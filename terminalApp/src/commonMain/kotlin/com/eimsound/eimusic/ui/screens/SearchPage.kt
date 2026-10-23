package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.SearchFocus
import com.eimsound.eimusic.keyboard.dispatchSearchKey
import com.eimsound.eimusic.keyboard.searchStatusBarHint
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.ui.components.InputField
import com.eimsound.eimusic.ui.components.LocalTrackActionHost
import com.eimsound.eimusic.ui.components.ListView
import com.eimsound.eimusic.ui.components.PlaylistPicker
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.components.TrackListItem
import com.eimsound.eimusic.ui.components.rememberListSelectionState
import com.eimsound.eimusic.ui.components.rememberPlaylistPickerState
import com.eimsound.eimusic.ui.components.trackContextItems
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.bridgeToMutable
import com.eimsound.eimusic.util.render
import com.eimsound.eimusic.util.spacer
import com.eimsound.eimusic.viewmodel.PlaylistViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.SearchViewModel
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.clickable
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.layout.onPreviewKeyEvent
import com.jakewharton.mosaic.layout.padding
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text

@Composable
fun SearchPage(
    searchViewModel: SearchViewModel,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel,
    onGoBack: () -> Unit,
) {
    val searchState by searchViewModel.state.bridgeToMutable()
    var focus by remember { mutableStateOf(SearchFocus.Input) }
    val resultSelection = rememberListSelectionState()
    val historySelection = rememberListSelectionState()
    val picker = rememberPlaylistPickerState()
    val trackActions = LocalTrackActionHost.current
    val playlistState by playlistViewModel.state.collectAsState()
    val localPlaylists = playlistState.playlists.filter { it.id.source == TrackSource.LOCAL }
    val palette = LocalTuiColors.current
    val termWidth = LocalTerminalState.current.size.columns
    val termHeight = LocalTerminalState.current.size.rows
    val visibleLines = (termHeight - 12).coerceIn(3, 18)
    val columnWidth = (termWidth * 0.7f).toInt().coerceIn(30, termWidth - 4)

    LaunchedEffect(searchState.query) {
        if (searchState.query.isNotBlank() && focus == SearchFocus.History) focus = SearchFocus.Input
    }

    ScreenFrame(
        hint = searchStatusBarHint(focus),
        onKey = { event ->
            if (focus == SearchFocus.History) {
                if (event.key == "Escape") {
                    focus = SearchFocus.Input
                    true
                } else {
                    false // history ListView handles arrows/Enter
                }
            } else if (picker.isOpen) {
                if (event.key == "Escape") {
                    picker.close()
                    true
                } else {
                    false
                }
            } else if (event.key == "l" && focus == SearchFocus.Results && !event.ctrl && !event.alt) {
                searchState.results.getOrNull(resultSelection.index)?.let(picker::open)
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .width(columnWidth)
                    .onPreviewKeyEvent { event ->
                        if (
                            focus == SearchFocus.Input &&
                            searchState.query.isBlank() &&
                            searchState.recentSearches.isNotEmpty() &&
                            (event.key == "ArrowDown" || event.key == "ArrowUp")
                        ) {
                            focus = SearchFocus.History
                            historySelection.jumpTo(
                                if (event.key == "ArrowDown") 0 else searchState.recentSearches.lastIndex,
                            )
                            true
                        } else {
                            false
                        }
                    }
                    .padding(horizontal = 2, vertical = 1),
            ) {
            Text(
                " Search",
                modifier = Modifier.fillMaxWidth(),
                color = palette.titleFg,
                textAlign = Alignment.CenterHorizontally,
            )
            Text(
                "─".repeat((columnWidth - 6).coerceAtLeast(10)),
                color = palette.dimBorderFg,
            )
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
                ListView(
                    items = searchState.recentSearches.take(6),
                    visibleLines = (visibleLines - 2).coerceAtLeast(1),
                    emptyMessage = "",
                    enabled = focus == SearchFocus.History,
                    selection = historySelection,
                    onConfirm = { recent ->
                        searchViewModel.updateQuery(recent)
                        searchViewModel.submitSearch()
                        focus = SearchFocus.Results
                    },
                    render = { recent, _, selected ->
                        Text(
                            (if (selected) " ▸ " else "   ") + recent,
                            color = if (selected) palette.highlightFg else palette.textDimFg,
                        )
                    },
                )
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

            if (searchState.isLoading) {
                Text("  Searching...", color = palette.textDimFg)
            }
            searchState.error?.let {
                Text("  Error: $it", color = palette.errorFg)
            }

            PlaylistPicker(
                state = picker,
                playlists = localPlaylists,
                visibleLines = visibleLines,
            ) { playlist, track ->
                playlistViewModel.addTrackToPlaylist(playlist.id.id, track)
            }

            if (!picker.isOpen && !searchState.isLoading && searchState.results.isNotEmpty()) {
                Text(" ", color = palette.textFg)
                val focusLabel = if (focus == SearchFocus.Results) "▸ Results" else "Results"
                Text("  $focusLabel", color = palette.titleFg)
                Text(
                    "  + Add to playlist",
                    modifier = Modifier.clickable {
                        searchState.results.getOrNull(resultSelection.index)?.let(picker::open)
                    },
                    color = palette.highlightFg,
                )
                ListView(
                    items = searchState.results,
                    visibleLines = visibleLines,
                    emptyMessage = "",
                    selection = resultSelection,
                    onItemSecondaryClick = { track, _ ->
                        trackActions?.showMenu(
                            track.name,
                            trackContextItems(
                                onAddToPlaylist = { picker.open(track) },
                                onPlayNext = { playerViewModel.playNext(track) },
                                onShowInfo = { trackActions.showInfo(track) },
                            ),
                        )
                    },
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

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
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.bridgeToMutable
import com.eimsound.eimusic.util.render
import com.eimsound.eimusic.util.spacer
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
    onGoBack: () -> Unit,
) {
    val searchState by searchViewModel.state.bridgeToMutable()
    var focus by remember { mutableStateOf(SearchFocus.Input) }
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
                        dispatchSearchKey(
                            event = event,
                            focus = focus,
                            onSwitchToInput = { focus = SearchFocus.Input },
                        )
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

                // ── Results list ──
                if (!searchState.isLoading && searchState.results.isNotEmpty()) {
                    Text(" ", color = palette.textFg)
                    val focusLabel = if (focus == SearchFocus.Results) "▸ Results" else "Results"
                    Text("  $focusLabel", color = palette.titleFg)
                    ListView(
                        items = searchState.results,
                        visibleLines = visibleLines,
                        emptyMessage = "",
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

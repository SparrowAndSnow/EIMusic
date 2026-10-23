package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eimsound.eimusic.keyboard.TrackTableScreenAction
import com.eimsound.eimusic.keyboard.dispatchTrackTableKey
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Spacer
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextOverflow

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${if (s < 10) "0" else ""}$s"
}

/**
 * Track list with aligned columns.
 *
 * 列宽交给 Row 的固定宽度槽位，单元格内容由 Text 的 maxLines + Ellipsis 负责截断，
 * 不再手工按字符宽度 pad/truncate（宽字符与 emoji 由 Mosaic 统一度量）。
 */
@Composable
fun TrackTable(
    tracks: List<Track>,
    currentTrackId: TrackId?,
    termWidth: Int,
    visibleLines: Int,
    emptyMessage: String = " -- empty -- ",
    showScrollIndicator: Boolean = true,
    enabled: Boolean = true,
    onPlayTrack: (Track) -> Unit = {},
    onPlayAll: (List<Track>) -> Unit = {},
    onAddNext: (Track) -> Unit = {},
    onAddLast: (Track) -> Unit = {},
    onSelectedChange: (Int) -> Unit = {},
    onConfirm: (Track) -> Unit = {},
    rememberSelectionKey: Any? = tracks,
    initialSelectedIndex: Int = 0,
    controlledSelectedIndex: Int? = null,
) {
    var selectedIndex by remember(tracks) { mutableIntStateOf(0) }
    val palette = LocalTuiColors.current
    val numWidth = 4
    val nameWidth = (termWidth * 0.40).toInt().coerceIn(15, 40)
    val artistWidth = (termWidth * 0.25).toInt().coerceIn(10, 25)
    val durWidth = 8

    val uiInput = LocalUiInputEnabled.current
    Box(Modifier.onKeyEvent { event ->
        if (!enabled || !uiInput) return@onKeyEvent false
        val action = dispatchTrackTableKey(event, selectedIndex, { selectedIndex = it; onSelectedChange(it) }, tracks.lastIndex)
        when (action) {
            TrackTableScreenAction.PlayTrack -> { tracks.getOrNull(selectedIndex)?.let(onPlayTrack); true }
            TrackTableScreenAction.PlayAll -> { onPlayAll(tracks); true }
            TrackTableScreenAction.AddNext -> { tracks.getOrNull(selectedIndex)?.let(onAddNext); true }
            TrackTableScreenAction.AddLast -> { tracks.getOrNull(selectedIndex)?.let(onAddLast); true }
            null -> false
        }
    }) {
        if (tracks.isEmpty()) {
            if (emptyMessage.isNotEmpty()) {
                Text(" $emptyMessage", color = palette.textDimFg)
            }
            return@Box
        }

        Column {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(2))
                Box(Modifier.width(numWidth)) {
                    Text("#", Modifier.fillMaxWidth(), color = palette.textDimFg, softWrap = true, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(1))
                Box(Modifier.width(nameWidth)) {
                    Text("Title", Modifier.fillMaxWidth(), color = palette.textDimFg, softWrap = true, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.width(artistWidth)) {
                    Text("Artist", Modifier.fillMaxWidth(), color = palette.textDimFg, softWrap = true, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.width(durWidth)) {
                    Text(
                        "Duration",
                        Modifier.fillMaxWidth(),
                        color = palette.textDimFg,
                        textAlign = Alignment.End,
                        softWrap = true,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            ListView(
                items = tracks,
                visibleLines = visibleLines,
                enabled = enabled,
                emptyMessage = emptyMessage,
                showScrollIndicator = showScrollIndicator,
                onConfirm = onConfirm,
                onSelectedChange = onSelectedChange,
                rememberSelectionKey = rememberSelectionKey,
                initialSelectedIndex = initialSelectedIndex,
                controlledSelectedIndex = controlledSelectedIndex,
                render = { track, i, isSelected ->
                    val isCurrent = track.id == currentTrackId
                    val marker = if (isCurrent) "▶" else if (isSelected) "▸" else " "
                    val markerColor = when {
                        isCurrent -> palette.highlightFg
                        isSelected -> palette.currentMarkerFg
                        else -> Color.Unspecified
                    }
                    val nameColor = if (isCurrent) palette.highlightFg else palette.textFg
                    val artist = track.artists?.firstOrNull()?.name ?: ""
                    val dur = track.duration?.milliseconds?.let { formatDuration(it) } ?: "--:--"

                    val rowModifier = if (isSelected) {
                        Modifier.fillMaxWidth().background(palette.selectionBg)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                    Row(rowModifier) {
                        Text(marker, color = markerColor)
                        Spacer(Modifier.width(1))
                        Box(Modifier.width(numWidth)) {
                            Text(
                                "#${i + 1}",
                                Modifier.fillMaxWidth(),
                                color = palette.textDimFg,
                                softWrap = true,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(1))
                        Box(Modifier.width(nameWidth)) {
                            Text(
                                track.name,
                                Modifier.fillMaxWidth(),
                                color = nameColor,
                                softWrap = true,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(Modifier.width(artistWidth)) {
                            Text(
                                artist,
                                Modifier.fillMaxWidth(),
                                color = palette.textDimFg,
                                softWrap = true,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(Modifier.width(durWidth)) {
                            Text(
                                dur,
                                Modifier.fillMaxWidth(),
                                color = palette.textDimFg,
                                textAlign = Alignment.End,
                                softWrap = true,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
            )
        }
    }
}

package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.*
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.padEndVisual
import com.eimsound.eimusic.util.padStartVisual
import com.eimsound.eimusic.util.render
import com.eimsound.eimusic.util.spacer
import com.eimsound.eimusic.util.truncate
import com.eimsound.eimusic.util.truncated
import com.eimsound.eimusic.keyboard.TrackTableScreenAction
import com.eimsound.eimusic.keyboard.dispatchTrackTableKey
import com.eimsound.eimusic.keyboard.mapTrackTableKey
import com.jakewharton.mosaic.layout.*
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.*

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${if (s < 10) "0" else ""}$s"
}

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
) {
    var selectedIndex by remember(tracks) { mutableIntStateOf(0) }
    val palette = LocalTuiColors.current
    val numWidth = 4
    val nameWidth = (termWidth * 0.40).toInt().coerceIn(15, 40)
    val artistWidth = (termWidth * 0.25).toInt().coerceIn(10, 25)
    val durWidth = 7

    Box(Modifier.onKeyEvent { event ->
        if (!enabled) return@onKeyEvent false
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
            Text(buildAnnotatedString {
                spacer(2)
                withStyle(SpanStyle(color = palette.textDimFg)) {
                    append("#".padEnd(numWidth))
                    append(1.spacer)
                    append("Title".padEndVisual(nameWidth))
                    append("Artist".padEndVisual(artistWidth))
                    append("Duration".padStart(durWidth))
                }
            })

            ListView(
                items = tracks,
                visibleLines = visibleLines,
                enabled = enabled,
                emptyMessage = emptyMessage,
                showScrollIndicator = showScrollIndicator,
                onConfirm = onConfirm,
                onSelectedChange = onSelectedChange,
                render = { track, i, isSelected ->
                    val isCurrent = track.id == currentTrackId
                    val marker = if (isCurrent) "▶" else if (isSelected) "▸" else " "
                    val markerColor = when {
                        isCurrent -> palette.highlightFg
                        isSelected -> palette.currentMarkerFg
                        else -> Color.Unspecified
                    }
                    val name = track.name.render().truncate(nameWidth)
                    val artist = (track.artists?.firstOrNull()?.name ?: "").render().truncate(artistWidth)
                    val dur = track.duration?.milliseconds?.let { formatDuration(it) }?.padStart(5) ?: "  --:--"

                    Box(
                        modifier = if (isSelected) Modifier.fillMaxWidth()
                            .background(palette.selectionBg) else Modifier.fillMaxWidth()
                    ) {
                        Text(buildAnnotatedString {
                            withStyle(SpanStyle(color = markerColor)) { append(marker) }
                            spacer(1)
                            withStyle(SpanStyle(color = palette.textDimFg)) {
                                append(
                                    "#${
                                        (i + 1).toString().padEnd(numWidth)
                                    }"
                                )
                            }
                            withStyle(SpanStyle(color = if (isCurrent) palette.highlightFg else palette.textFg)) {
                                append(
                                    name.padEndVisual(nameWidth)
                                )
                            }
                            withStyle(SpanStyle(color = palette.textDimFg)) { append(artist.padEndVisual(artistWidth)) }
                            withStyle(SpanStyle(color = palette.textDimFg)) { append(dur.padStart(durWidth)) }
                        })
                    }
                },
            )
        }
    }
}

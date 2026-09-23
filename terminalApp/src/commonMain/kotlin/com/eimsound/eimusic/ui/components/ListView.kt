package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.ListViewScreenAction
import com.eimsound.eimusic.keyboard.dispatchListViewKey
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.render
import com.eimsound.eimusic.util.spacer
import com.eimsound.eimusic.util.truncate
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text

@Composable
fun <T> ListView(
    items: List<T>,
    visibleLines: Int,
    render: @Composable (item: T, index: Int, isSelected: Boolean) -> Unit,
    emptyMessage: String = " -- empty -- ",
    showScrollIndicator: Boolean = true,
    enabled: Boolean = true,
    onConfirm: (T) -> Unit = {},
    onSelectedChange: (Int) -> Unit = {},
) {
    var selectedIndex by remember(items) { mutableIntStateOf(0) }
    val palette = LocalTuiColors.current

    val scrollOffset = remember(selectedIndex, visibleLines, items.size) {
        val max = (items.size - visibleLines).coerceAtLeast(0)
        (selectedIndex - visibleLines / 2).coerceIn(0, max)
    }

    Box(Modifier.onKeyEvent { event ->
        if (!enabled) return@onKeyEvent false
        val action =
            dispatchListViewKey(event, selectedIndex, { selectedIndex = it; onSelectedChange(it) }, items.lastIndex)
        when (action) {
            ListViewScreenAction.Confirm -> {
                items.getOrNull(selectedIndex)?.let(onConfirm); true
            }
            null -> false
        }
    }) {
        if (items.isEmpty()) {
            Text(emptyMessage, color = palette.textDimFg)
            return@Box
        }

        Column {
            val end = (scrollOffset + visibleLines).coerceAtMost(items.size)
            val shown = end - scrollOffset

            for (i in scrollOffset until end) {
                val item = items[i]
                val isSelected = i == selectedIndex
                render(item, i, isSelected)
            }

            if (showScrollIndicator && items.size > visibleLines) {
                val total = items.size
                val posLabel = "${scrollOffset + 1}-${end}/$total"
                repeat((visibleLines - shown).coerceAtLeast(0)) { Text("", color = palette.textDimFg) }
                Text("${2.spacer}$posLabel", color = palette.textDimFg)
            }
        }
    }
}

/**
 * Default list item renderer: yazi-style with `▸` for selected,
 * ` ` for unselected, index number, name, and artist.
 */
@Composable
fun TrackListItem(
    index: Int,
    isSelected: Boolean,
    isCurrent: Boolean,
    number: Int,
    name: String,
    artist: String,
    nameMax: Int = 50,
    artistMax: Int = 24,
) {
    val palette = LocalTuiColors.current
    val markerSelected = if (isSelected) "▸" else 1.spacer
    val markerCurrent = if (isCurrent) "-" else 1.spacer
    val idx = "#$number"
    val truncatedName = name.render().truncate(nameMax)
    val truncatedArtist = artist.render().truncate(artistMax)

    Text(
        buildAnnotatedString {
            if (isSelected || isCurrent) {
                if (isSelected) {
                    withStyle(SpanStyle(color = palette.currentMarkerFg)) { append(markerSelected) }
                } else if (isCurrent) {
                    withStyle(SpanStyle(color = palette.currentMarkerFg)) { append(markerCurrent) }
                }
                spacer(1)
                withStyle(SpanStyle(color = palette.highlightFg)) { append(idx) }
                spacer(1)
                append(truncatedName)
                if (truncatedArtist.isNotEmpty()) append("  —  $truncatedArtist")
            } else {
                append("$markerSelected $idx $truncatedName")
                if (truncatedArtist.isNotEmpty()) append("  —  $truncatedArtist")
            }
        },
        color = if (isSelected || isCurrent) palette.highlightFg else palette.textFg,
    )
}

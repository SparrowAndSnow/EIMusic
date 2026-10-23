package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.layout.MouseEvent
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.onGloballyPositioned
import com.jakewharton.mosaic.layout.onMouseEvent
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Arrangement
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Text

/** Returns the filled/empty bar characters as a plain string. */
fun barString(
    filled: Int,
    width: Int,
    filledChar: String = "▓",
    emptyChar: String = "░",
): String = filledChar.repeat(filled.coerceIn(0, width)) + emptyChar.repeat((width - filled).coerceAtLeast(0))

@Composable
fun ProgressBar(
    current: Long,
    total: Long,
    width: Int,
    filledFg: State<Color>? = null,
    emptyFg: State<Color>? = null,
    onSeek: ((Long) -> Unit)? = null,
) {
    val palette = LocalTuiColors.current
    val filledColor = filledFg?.value ?: palette.progressFilledFg
    val emptyColor = emptyFg?.value ?: palette.progressEmptyFg

    if (width <= 0) return

    var barX by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }

    fun seekAt(x: Int) {
        val seek = onSeek ?: return
        if (total <= 0L) return
        val column = (x - barX).coerceIn(0, width - 1)
        val ratio = if (width <= 1) 0f else column.toFloat() / (width - 1)
        seek((ratio * total).toLong())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .onGloballyPositioned { position, _ -> barX = position.x }
                .then(
                    if (onSeek == null) {
                        Modifier
                    } else {
                        Modifier.onMouseEvent { event ->
                            when (event.type) {
                                MouseEvent.Type.Press -> {
                                    if (event.button == MouseEvent.Button.Left) {
                                        dragging = true
                                        seekAt(event.x)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                MouseEvent.Type.Drag, MouseEvent.Type.Motion -> {
                                    if (dragging) {
                                        seekAt(event.x)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                MouseEvent.Type.Release -> {
                                    if (dragging) {
                                        dragging = false
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }
                        }
                    },
                ),
        ) {
            Text(
                buildAnnotatedString {
                    val ratio = if (total > 0L) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    val filled = (ratio * width).toInt()
                    val bar = barString(filled, width)
                    withStyle(SpanStyle(color = filledColor)) { append(bar.take(filled)) }
                    withStyle(SpanStyle(color = emptyColor)) { append(bar.drop(filled)) }
                },
                color = palette.textDimFg,
            )
        }
        Text(
            "  ${formatTime(current)} / ${formatTime(total)}",
            color = palette.textDimFg,
        )
    }
}

internal fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000L
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
}

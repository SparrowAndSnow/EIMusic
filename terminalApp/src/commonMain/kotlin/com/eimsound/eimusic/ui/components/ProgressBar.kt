package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Color
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
) {
    val palette = LocalTuiColors.current
    val filledColor = filledFg?.value ?: palette.progressFilledFg
    val emptyColor = emptyFg?.value ?: palette.progressEmptyFg

    if (width <= 0) return
    Text(
        buildAnnotatedString {
            val ratio = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val filled = (ratio * width).toInt()
            val bar = barString(filled, width)
            withStyle(SpanStyle(color = filledColor)) { append(bar.take(filled)) }
            withStyle(SpanStyle(color = emptyColor)) { append(bar.drop(filled)) }
            withStyle(SpanStyle(color = palette.textDimFg)) {
                append("  ${formatTime(current)} / ${formatTime(total)}")
            }
        },
        color = palette.textDimFg,
    )
}

internal fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000L
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
}

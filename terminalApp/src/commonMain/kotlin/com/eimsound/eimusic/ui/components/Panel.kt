package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Text

/**
 * Yazi-style split-panel layout using Row.
 *
 * [left] and [right] composables are placed side-by-side separated by a
 * vertical divider. The [isLeftActive] flag controls which panel gets the
 * active background tint.
 *
 * Uses explicit column widths (not Row weights) to avoid Mosaic's
 * constraint issue with weighted children at startup.
 */
@Composable
fun SplitPanel(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    leftRatio: Float = 0.45f,
    isLeftActive: Boolean = true,
) {
    val palette = LocalTuiColors.current

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .background(if (isLeftActive) palette.surfaceBg else palette.mainBg)
        ) {
            left()
        }

        Text("│", color = palette.borderFg)

        Box(
            modifier = Modifier
                .background(if (!isLeftActive) palette.surfaceBg else palette.mainBg)
        ) {
            right()
        }
    }
}

package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.layout.border
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.spacer
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.widthIn
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.ColumnScope
import com.jakewharton.mosaic.ui.Text

@Composable
fun Dialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalTuiColors.current
    val termWidth = LocalTerminalState.current.size.columns
    val minWidth = (termWidth * 0.35).toInt().coerceIn(30, 50)

    Column(
        modifier = modifier.widthIn(min = minWidth).background(palette.dialogBg).border(color = palette.activeBorderFg)
    ) {
        Text("${1.spacer}$title", color = palette.titleFg)
        content()
    }
}

package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.dispatchLogViewerKey
import com.eimsound.eimusic.keyboard.logViewerStatusBarHint
import com.eimsound.eimusic.ui.components.ListView
import com.eimsound.eimusic.ui.components.LocalStatusBarHint
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.ui.uiKeyEvent
import com.eimsound.eimusic.util.TuiLogger
import com.eimsound.eimusic.util.render
import com.jakewharton.mosaic.layout.*
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text

@Composable
fun LogViewerScreen(
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val safeArea = LocalSafeContentSize.current
    val statusBarHint = LocalStatusBarHint.current
    val uiActive = LocalUiInputEnabled.current
    LaunchedEffect(uiActive) {
        statusBarHint.value = logViewerStatusBarHint()
    }

    val termHeight = safeArea.rows
    val termWidth = safeArea.columns
    val wrapWidth = (termWidth - 4).coerceAtLeast(20)
    val logs by TuiLogger.entriesFlow.collectAsState()
    val wrappedLines = remember(logs) {
        logs.flatMap { log ->
            if (log.length <= wrapWidth) {
                listOf(log)
            } else {
                log.chunked(wrapWidth)
            }
        }
    }
    val rows = (termHeight - 4).coerceAtLeast(1)

    Box(
        modifier = Modifier.fillMaxSize().uiKeyEvent { event ->
            dispatchLogViewerKey(
                event = event,
                onClear = { TuiLogger.clear() },
                onClose = onGoBack,
            )
        }
    ) {
        Column {
            Text("Logs (Esc to close, j/k to scroll)", color = palette.titleFg)
            Text("", color = palette.textFg)
            ListView(
                items = wrappedLines,
                visibleLines = rows,
                emptyMessage = "",
                render = { line, i, isSelected ->
                    val marker = if (isSelected) "▸" else " "
                    val markerColor = if (isSelected) palette.currentMarkerFg else Color.Unspecified
                    Box(modifier = if (isSelected) Modifier.fillMaxWidth().background(palette.selectionBg) else Modifier.fillMaxWidth()) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = markerColor)) { append(marker) }
                                append(" ")
                                append(line.render())
                            },
                            color = palette.textFg,
                        )
                    }
                }
            )
        }
    }
}

package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.InputFieldScreenAction
import com.eimsound.eimusic.keyboard.dispatchInputFieldKey
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.layout.widthIn
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text

/**
 * Text input field with cursor indicator, optional placeholder, line wrapping,
 * and self-managed cursor positioning.
 *
 * Handles its own key events: typing, backspace, arrow keys for cursor movement,
 * Enter triggers [onConfirm], Escape triggers [onCancel].
 */
@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    showCursor: Boolean = true,
    enabled: Boolean = true,
    maxWidth: Int? = null,
    maxLines: Int = 3,
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    var cursorPosition by remember { mutableIntStateOf(value.length) }
    if (cursorPosition > value.length) cursorPosition = value.length.coerceAtLeast(0)

    // Mosaic drains all pending key events in a single frame before recomposition.
    // IME-committed characters arrive in rapid succession, so the composable parameter
    // `value` is stale within the batch. A mutable ref keeps successive handler
    // invocations chained correctly.
    val currentValue = remember { mutableStateOf(value) }
    if (value != currentValue.value) {
        currentValue.value = value
        if (cursorPosition > value.length) cursorPosition = value.length.coerceAtLeast(0)
    }

    Box(Modifier.onKeyEvent { event ->
        if (!enabled) return@onKeyEvent false

        // 计算当前光标所在的行和列（用于上下导航）
        val tempWrapWidth = maxWidth ?: 60
        val allLines = splitIntoLines(currentValue.value, tempWrapWidth)

        var currentLineIndex = 0
        var currentColInLine = cursorPosition
        var charCount = 0
        for (i in allLines.indices) {
            if (charCount + allLines[i].length >= cursorPosition) {
                currentLineIndex = i
                currentColInLine = cursorPosition - charCount
                break
            }
            charCount += allLines[i].length
        }

        val action = dispatchInputFieldKey(event, currentValue.value, { newValue ->
            currentValue.value = newValue; onValueChange(newValue)
        }, cursorPosition, { newPosition ->
            cursorPosition = newPosition
        }, currentLineIndex, currentColInLine, allLines, tempWrapWidth)
        
        when (action) {
            InputFieldScreenAction.Confirm -> { onConfirm(); true }
            InputFieldScreenAction.Cancel -> { onCancel(); true }
            null -> true
        }
    }) {
        val palette = LocalTuiColors.current
        val safeArea = LocalSafeContentSize.current
        val termWidth = safeArea.columns
        val contentWidth = (termWidth * 0.7f).toInt().coerceIn(30, termWidth - 4)

        if (currentValue.value.isEmpty() && placeholder.isNotEmpty()) {
            val sb = buildAnnotatedString {
                withStyle(SpanStyle(color = palette.textDimFg)) { append(placeholder) }
                if (showCursor) withStyle(SpanStyle(color = palette.highlightFg)) { append("█") }
            }
            Text(sb, color = palette.textFg)
            return@Box
        }

        val wrapWidth = maxWidth ?: contentWidth
        val lines = splitIntoLines(currentValue.value, wrapWidth)
        val displayLines = lines.takeLast(maxLines)

        val linesBeforeDisplay = lines.size - displayLines.size
        val charsBeforeDisplay = lines.take(linesBeforeDisplay).sumOf { it.length }
        val cursorInDisplay = (cursorPosition - charsBeforeDisplay).coerceIn(0, displayLines.sumOf { it.length.coerceAtLeast(0) })

        var cursorLine = displayLines.lastIndex
        var cursorCol = displayLines.lastOrNull()?.length ?: 0
        if (showCursor) {
            var remaining = cursorInDisplay
            for (i in displayLines.indices) {
                if (remaining <= displayLines[i].length) {
                    cursorLine = i
                    cursorCol = remaining
                    break
                }
                remaining -= displayLines[i].length
            }
        }

        Column(modifier = Modifier.widthIn(wrapWidth.coerceAtMost(30), (termWidth - 2).coerceAtLeast(1))) {
            displayLines.forEachIndexed { index, line ->
                Text(
                    buildAnnotatedString {
                        if (showCursor && index == cursorLine) {
                            withStyle(SpanStyle(color = palette.textFg)) { append(line.take(cursorCol)) }
                            withStyle(SpanStyle(color = palette.highlightFg)) { append("█") }
                            withStyle(SpanStyle(color = palette.textFg)) { append(line.drop(cursorCol)) }
                        } else {
                            withStyle(SpanStyle(color = palette.textFg)) { append(line) }
                        }
                    },
                    color = palette.textFg,
                )
            }
        }
    }
}

/** 将文本按换行符和 [maxWidth] 字符长度分行为多行 */
fun splitIntoLines(text: String, maxWidth: Int): List<String> =
    if (maxWidth <= 0 || text.length <= maxWidth && '\n' !in text) listOf(text)
    else text.lines().flatMap { line ->
        if (line.length <= maxWidth) listOf(line)
        else line.chunked(maxWidth)
    }


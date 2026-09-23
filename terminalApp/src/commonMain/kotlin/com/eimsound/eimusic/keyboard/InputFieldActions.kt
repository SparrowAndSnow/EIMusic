package com.eimsound.eimusic.keyboard

import com.jakewharton.mosaic.layout.KeyEvent

sealed interface InputFieldBindingAction : KeyAction {
    data object Confirm : InputFieldBindingAction
    data object Cancel : InputFieldBindingAction
    data object DeleteChar : InputFieldBindingAction
    data object DeleteForward : InputFieldBindingAction
    data object MoveCursorLeft : InputFieldBindingAction
    data object MoveCursorRight : InputFieldBindingAction
    data object MoveCursorUp : InputFieldBindingAction
    data object MoveCursorDown : InputFieldBindingAction
}

val inputFieldKeyBindings: List<KeyBinding<InputFieldBindingAction>> = listOf(
    KeyBinding(listOf("Enter"), "Enter", InputFieldBindingAction.Confirm, "Confirm"),
    KeyBinding(listOf("Escape"), "Esc", InputFieldBindingAction.Cancel, "Cancel"),
    KeyBinding(listOf("Backspace"), "⌫", InputFieldBindingAction.DeleteChar, "Delete"),
    KeyBinding(listOf("Delete"), "Del", InputFieldBindingAction.DeleteForward, "Delete forward"),
    KeyBinding(listOf("ArrowLeft"), "←", InputFieldBindingAction.MoveCursorLeft, "Cursor left"),
    KeyBinding(listOf("ArrowRight"), "→", InputFieldBindingAction.MoveCursorRight, "Cursor right"),
    KeyBinding(listOf("ArrowUp"), "↑", InputFieldBindingAction.MoveCursorUp, "Cursor up"),
    KeyBinding(listOf("ArrowDown"), "↓", InputFieldBindingAction.MoveCursorDown, "Cursor down"),
)

private val inputFieldKeyMap: Map<String, InputFieldBindingAction> = buildMap {
    inputFieldKeyBindings.forEach { binding ->
        binding.keys.forEach { key -> put(key, binding.action) }
    }
}

fun mapInputFieldKey(key: String): InputFieldBindingAction? = inputFieldKeyMap[key]

enum class InputFieldScreenAction { Confirm, Cancel }

/**
 * Dispatches a key event for an InputField.
 *
 * Uses [mapInputFieldKey] for known actions (Confirm, Cancel, DeleteChar, DeleteForward, cursor movement).
 * Treats single-character keys as text input inserted at [cursorPosition].
 */
fun dispatchInputFieldKey(
    event: KeyEvent,
    value: String,
    onValueChange: (String) -> Unit,
    cursorPosition: Int = value.length,
    onCursorChange: (Int) -> Unit = {},
    currentLineIndex: Int = 0,
    currentColInLine: Int = 0,
    allLines: List<String> = emptyList(),
    wrapWidth: Int = 60,
): InputFieldScreenAction? {
    val action = mapInputFieldKey(event.key)
    return when (action) {
        InputFieldBindingAction.Confirm -> InputFieldScreenAction.Confirm
        InputFieldBindingAction.Cancel -> InputFieldScreenAction.Cancel
        InputFieldBindingAction.DeleteChar -> {
            // Backspace: 删除光标前的字符（支持代理对，如 emoji）
            val safePos = cursorPosition.coerceIn(0, value.length)
            if (safePos > 0) {
                val deleteStart = if (safePos >= 2 && value[safePos - 1].isLowSurrogate()) safePos - 2 else safePos - 1
                onValueChange(value.substring(0, deleteStart) + value.substring(safePos))
                onCursorChange(deleteStart)
            }
            null
        }

        InputFieldBindingAction.DeleteForward -> {
            // Del: 删除光标后的字符（支持代理对，如 emoji）
            val safePos = cursorPosition.coerceIn(0, value.length)
            if (safePos < value.length) {
                val deleteEnd = if (safePos + 1 < value.length && value[safePos].isHighSurrogate()) safePos + 2 else safePos + 1
                onValueChange(value.substring(0, safePos) + value.substring(deleteEnd))
                // 光标位置不变
            }
            null
        }

        InputFieldBindingAction.MoveCursorLeft -> {
            val newPos = if (cursorPosition >= 2 && value[cursorPosition - 1].isLowSurrogate())
                cursorPosition - 2
            else
                (cursorPosition - 1).coerceAtLeast(0)
            onCursorChange(newPos)
            null
        }

        InputFieldBindingAction.MoveCursorRight -> {
            val newPos = if (cursorPosition < value.length && value[cursorPosition].isHighSurrogate())
                cursorPosition + 2
            else
                (cursorPosition + 1).coerceAtMost(value.length)
            onCursorChange(newPos)
            null
        }

        InputFieldBindingAction.MoveCursorUp -> {
            // 向上移动一行
            if (currentLineIndex > 0) {
                val prevLine = allLines[currentLineIndex - 1]
                // 保持列位置，但不超过上一行的长度
                val newCol = currentColInLine.coerceAtMost(prevLine.length)
                // 计算新的绝对位置
                var newPos = 0
                for (i in 0 until currentLineIndex - 1) {
                    newPos += allLines[i].length
                }
                newPos += newCol
                onCursorChange(newPos.coerceIn(0, value.length))
            }
            null
        }

        InputFieldBindingAction.MoveCursorDown -> {
            // 向下移动一行
            if (currentLineIndex < allLines.size - 1) {
                val nextLine = allLines[currentLineIndex + 1]
                // 保持列位置，但不超过下一行的长度
                val newCol = currentColInLine.coerceAtMost(nextLine.length)
                // 计算新的绝对位置
                var newPos = 0
                for (i in 0..currentLineIndex) {
                    newPos += allLines[i].length
                }
                newPos += newCol
                onCursorChange(newPos.coerceIn(0, value.length))
            }
            null
        }

        null -> {
            val safePos = cursorPosition.coerceIn(0, value.length)
            onValueChange(value.substring(0, safePos) + event.key + value.substring(safePos))
            onCursorChange(safePos + event.key.length)
            null
        }
    }
}

fun inputFieldStatusBarHint(): String = inputFieldKeyBindings
    .filter { it.action in setOf(InputFieldBindingAction.Confirm, InputFieldBindingAction.Cancel) }
    .joinToString("  ") { "${it.display}:${it.description}" }

val inputFieldHelpEntries: List<Pair<String, String>> = inputFieldKeyBindings
    .distinctBy { it.display }
    .map { it.display to it.description }

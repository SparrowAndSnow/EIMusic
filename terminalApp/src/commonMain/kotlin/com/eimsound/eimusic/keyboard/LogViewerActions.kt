package com.eimsound.eimusic.keyboard

import com.jakewharton.mosaic.layout.KeyEvent

sealed interface LogViewerAction : KeyAction {
    data object Clear : LogViewerAction
    data object Close : LogViewerAction
}

val logViewerKeyBindings: List<KeyBinding<LogViewerAction>> = listOf(
    KeyBinding(listOf("c"), "c", LogViewerAction.Clear, "Clear"),
    KeyBinding(listOf("Escape", "`"), "Esc", LogViewerAction.Close, "Close"),
)

private val logViewerKeyMap: Map<String, LogViewerAction> = buildMap {
    logViewerKeyBindings.forEach { b -> b.keys.forEach { k -> put(k, b.action) } }
}

fun mapLogViewerKey(key: String): LogViewerAction? = logViewerKeyMap[key]

fun dispatchLogViewerKey(
    event: KeyEvent,
    onClear: () -> Unit,
    onClose: () -> Unit,
): Boolean {
    val screenAction = mapLogViewerKey(event.key)
    return when (screenAction) {
        LogViewerAction.Clear -> { onClear(); true }
        LogViewerAction.Close -> { onClose(); true }
        null -> false
    }
}

fun logViewerStatusBarHint(): String = listViewKeyBindings
    .joinToString("  ") { "${it.display}:${it.description}" } + "  " +
    logViewerKeyBindings.joinToString("  ") { "${it.display}:${it.description}" }

package com.eimsound.eimusic.keyboard

import com.jakewharton.mosaic.layout.KeyEvent

sealed interface QueueAction : KeyAction {
    data object RemoveTrack : QueueAction
    data object ClearQueue : QueueAction
    data object GoBack : QueueAction
}

val queueKeyBindings: List<KeyBinding<QueueAction>> = listOf(
    KeyBinding(listOf("d"), "d", QueueAction.RemoveTrack, "Remove"),
    KeyBinding(listOf("c"), "c", QueueAction.ClearQueue, "Clear"),
    KeyBinding(listOf("Escape"), "Esc", QueueAction.GoBack, "Back"),
)

private val queueKeyMap: Map<String, QueueAction> = buildMap {
    queueKeyBindings.forEach { b -> b.keys.forEach { k -> put(k, b.action) } }
}

fun mapQueueKey(key: String): QueueAction? = queueKeyMap[key]

fun dispatchQueueKey(
    event: KeyEvent,
    onRemoveTrack: () -> Unit,
    onClearQueue: () -> Unit,
    onGoBack: () -> Unit,
): Boolean {
    val screenAction = mapQueueKey(event.key)
    return when (screenAction) {
        QueueAction.RemoveTrack -> { onRemoveTrack(); true }
        QueueAction.ClearQueue -> { onClearQueue(); true }
        QueueAction.GoBack -> { onGoBack(); true }
        null -> false // TrackTable handles its own keys
    }
}

fun queueStatusBarHint(): String = trackTableKeyBindings
    .filter { it.action in setOf(TrackTableBindingAction.ScrollDown, TrackTableBindingAction.ScrollUp, TrackTableBindingAction.PlayTrack) }
    .joinToString("  ") { "${it.display}:${it.description}" } + "  " +
    queueKeyBindings.joinToString("  ") { "${it.display}:${it.description}" }

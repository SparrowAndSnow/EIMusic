package com.eimsound.eimusic.keyboard

import com.jakewharton.mosaic.layout.KeyEvent

sealed interface QueueAction : KeyAction {
    data object MoveUp : QueueAction
    data object MoveDown : QueueAction
    data object JumpToCurrent : QueueAction
    data object OpenMenu : QueueAction
    data object RemoveTrack : QueueAction
    data object ClearQueue : QueueAction
    data object GoBack : QueueAction
}

val queueKeyBindings: List<KeyBinding<QueueAction>> = listOf(
    KeyBinding(listOf("K"), "K", QueueAction.MoveUp, "Move up"),
    KeyBinding(listOf("J"), "J", QueueAction.MoveDown, "Move down"),
    KeyBinding(listOf("g"), "g", QueueAction.JumpToCurrent, "Current"),
    KeyBinding(listOf("m"), "m", QueueAction.OpenMenu, "Menu"),
    KeyBinding(listOf("d"), "d", QueueAction.RemoveTrack, "Remove"),
    KeyBinding(listOf("c"), "c", QueueAction.ClearQueue, "Clear"),
    KeyBinding(listOf("Escape"), "Esc", QueueAction.GoBack, "Back"),
)

private val queueKeyMap: Map<String, QueueAction> = buildMap {
    queueKeyBindings.forEach { b -> b.keys.forEach { k -> put(k, b.action) } }
}

fun mapQueueKey(key: String): QueueAction? = queueKeyMap[key]

/** Queue 页面按键回调集合。 */
data class QueueKeyActions(
    val moveUp: () -> Unit,
    val moveDown: () -> Unit,
    val jumpToCurrent: () -> Unit,
    val openMenu: () -> Unit,
    val removeTrack: () -> Unit,
    val clearQueue: () -> Unit,
    val goBack: () -> Unit,
)

fun dispatchQueueKey(
    event: KeyEvent,
    actions: QueueKeyActions,
): Boolean {
    if (event.ctrl || event.alt) return false // Ctrl-C 等交给全局退出
    val screenAction = mapQueueKey(event.key)
    return when (screenAction) {
        QueueAction.MoveUp -> { actions.moveUp(); true }
        QueueAction.MoveDown -> { actions.moveDown(); true }
        QueueAction.JumpToCurrent -> { actions.jumpToCurrent(); true }
        QueueAction.OpenMenu -> { actions.openMenu(); true }
        QueueAction.RemoveTrack -> { actions.removeTrack(); true }
        QueueAction.ClearQueue -> { actions.clearQueue(); true }
        QueueAction.GoBack -> { actions.goBack(); true }
        null -> false // TrackTable handles its own keys
    }
}

fun queueStatusBarHint(): String = trackTableKeyBindings
    .filter { it.action in setOf(TrackTableBindingAction.ScrollDown, TrackTableBindingAction.ScrollUp, TrackTableBindingAction.PlayTrack) }
    .joinToString("  ") { "${it.display}:${it.description}" } + "  " +
    queueKeyBindings.joinToString("  ") { "${it.display}:${it.description}" }

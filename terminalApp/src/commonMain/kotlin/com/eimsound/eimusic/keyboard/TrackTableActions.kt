package com.eimsound.eimusic.keyboard

import com.jakewharton.mosaic.layout.KeyEvent

sealed interface TrackTableBindingAction : KeyAction {
    data object ScrollDown : TrackTableBindingAction
    data object ScrollUp : TrackTableBindingAction
    data object PlayTrack : TrackTableBindingAction
    data object AddNext : TrackTableBindingAction
    data object AddLast : TrackTableBindingAction
}

val trackTableKeyBindings: List<KeyBinding<TrackTableBindingAction>> = listOf(
    KeyBinding(listOf("j", "ArrowDown"), "↓", TrackTableBindingAction.ScrollDown, "Scroll down"),
    KeyBinding(listOf("k", "ArrowUp"), "↑", TrackTableBindingAction.ScrollUp, "Scroll up"),
    KeyBinding(listOf("Enter"), "Enter", TrackTableBindingAction.PlayTrack, "Play"),
    KeyBinding(listOf("n"), "n", TrackTableBindingAction.AddNext, "Add next"),
    KeyBinding(listOf("a"), "a", TrackTableBindingAction.AddLast, "Add last"),
)

private val trackTableKeyMap: Map<String, TrackTableBindingAction> = buildMap {
    trackTableKeyBindings.forEach { binding ->
        binding.keys.forEach { key -> put(key, binding.action) }
    }
}

fun mapTrackTableKey(key: String): TrackTableBindingAction? = trackTableKeyMap[key]

enum class TrackTableScreenAction { PlayTrack, AddNext, AddLast }

fun dispatchTrackTableKey(
    event: KeyEvent,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    lastIndex: Int,
): TrackTableScreenAction? {
    val action = mapTrackTableKey(event.key)
    return when (action) {
        TrackTableBindingAction.ScrollDown -> {
            if (lastIndex >= 0) onSelectedIndexChange((selectedIndex + 1).coerceAtMost(lastIndex))
            null
        }
        TrackTableBindingAction.ScrollUp -> {
            onSelectedIndexChange((selectedIndex - 1).coerceAtLeast(0))
            null
        }
        TrackTableBindingAction.PlayTrack -> TrackTableScreenAction.PlayTrack
        TrackTableBindingAction.AddNext -> TrackTableScreenAction.AddNext
        TrackTableBindingAction.AddLast -> TrackTableScreenAction.AddLast
        null -> null
    }
}

fun trackTableStatusBarHint(): String = trackTableKeyBindings
    .filter { it.action in setOf(TrackTableBindingAction.ScrollDown, TrackTableBindingAction.ScrollUp, TrackTableBindingAction.PlayTrack) }
    .joinToString("  ") { "${it.display}:${it.description}" }

val trackTableHelpEntries: List<Pair<String, String>> = trackTableKeyBindings
    .distinctBy { it.display }
    .map { it.display to it.description }

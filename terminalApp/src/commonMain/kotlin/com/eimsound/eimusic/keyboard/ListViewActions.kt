package com.eimsound.eimusic.keyboard

import com.eimsound.eimusic.i18n.t

import com.jakewharton.mosaic.layout.KeyEvent

sealed interface ListViewBindingAction : KeyAction {
    data object ScrollDown : ListViewBindingAction
    data object ScrollUp : ListViewBindingAction
    data object Confirm : ListViewBindingAction
}

val listViewKeyBindings: List<KeyBinding<ListViewBindingAction>> = listOf(
    KeyBinding(listOf("j", "ArrowDown"), "↓", ListViewBindingAction.ScrollDown, "Scroll down"),
    KeyBinding(listOf("k", "ArrowUp"), "↑", ListViewBindingAction.ScrollUp, "Scroll up"),
    KeyBinding(listOf("Enter"), "Enter", ListViewBindingAction.Confirm, "Confirm"),
)

private val listViewKeyMap: Map<String, ListViewBindingAction> = buildMap {
    listViewKeyBindings.forEach { binding ->
        binding.keys.forEach { key -> put(key, binding.action) }
    }
}

fun mapListViewKey(key: String): ListViewBindingAction? = listViewKeyMap[key]

enum class ListViewScreenAction { Confirm }

fun dispatchListViewKey(
    event: KeyEvent,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    lastIndex: Int,
): ListViewScreenAction? {
    val action = mapListViewKey(event.key)
    return when (action) {
        ListViewBindingAction.ScrollDown -> {
            if (lastIndex >= 0) onSelectedIndexChange((selectedIndex + 1).coerceAtMost(lastIndex))
            null
        }
        ListViewBindingAction.ScrollUp -> {
            onSelectedIndexChange((selectedIndex - 1).coerceAtLeast(0))
            null
        }
        ListViewBindingAction.Confirm -> ListViewScreenAction.Confirm
        null -> null
    }
}

fun listViewStatusBarHint(): String = t("hint.listview")

val listViewHelpEntries: List<Pair<String, String>> = listViewKeyBindings
    .distinctBy { it.display }
    .map { it.display to it.description }

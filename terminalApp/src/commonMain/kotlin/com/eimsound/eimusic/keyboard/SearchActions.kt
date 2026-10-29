package com.eimsound.eimusic.keyboard

import com.eimsound.eimusic.i18n.t

import com.jakewharton.mosaic.layout.KeyEvent

// ── Screen focus ──

enum class SearchFocus { Input, History, Results }

// ── Screen-specific actions ──

sealed interface SearchAction : KeyAction {
    data object SwitchToInput : SearchAction
}

val searchKeyBindings: List<KeyBinding<SearchAction>> = listOf(
    KeyBinding(listOf("Escape"), "Esc", SearchAction.SwitchToInput, "Back to input"),
)

private val searchKeyMap: Map<String, SearchAction> = buildMap {
    searchKeyBindings.forEach { b -> b.keys.forEach { k -> put(k, b.action) } }
}

fun mapSearchKey(key: String): SearchAction? = searchKeyMap[key]

// ── Dispatch ──

fun dispatchSearchKey(
    event: KeyEvent,
    focus: SearchFocus,
    onSwitchToInput: () -> Unit = {},
): Boolean = when (focus) {
    SearchFocus.Input, SearchFocus.History -> false
    SearchFocus.Results -> {
        when (mapSearchKey(event.key)) {
            SearchAction.SwitchToInput -> { onSwitchToInput(); true }
            null -> false // ListView handles its own keys
        }
    }
}

fun searchStatusBarHint(focus: SearchFocus): String = when (focus) {
    SearchFocus.Input -> t("hint.search.input")
    SearchFocus.History -> t("hint.search.history")
    SearchFocus.Results -> t("hint.search.results")
}

package com.eimsound.eimusic.keyboard

import com.jakewharton.mosaic.layout.KeyEvent

// ── Focus ──

enum class LocalFocus { Songs, Paths, Input }

// ── Screen-specific actions ──

sealed interface LocalSongsAction : KeyAction {
    data object Refresh : LocalSongsAction
    data object TogglePaths : LocalSongsAction
    data object AddPath : LocalSongsAction
    data object DeletePath : LocalSongsAction
    data object GoBack : LocalSongsAction
}

val localSongsKeyBindings: List<KeyBinding<LocalSongsAction>> = listOf(
    KeyBinding(listOf("r"), "r", LocalSongsAction.Refresh, "Refresh"),
    KeyBinding(listOf("/"), "/", LocalSongsAction.TogglePaths, "Manage paths"),
    KeyBinding(listOf("a"), "a", LocalSongsAction.AddPath, "Add path"),
    KeyBinding(listOf("d"), "d", LocalSongsAction.DeletePath, "Delete path"),
    KeyBinding(listOf("Escape"), "Esc", LocalSongsAction.GoBack, "Back"),
)

private val localSongsKeyMap: Map<String, LocalSongsAction> = buildMap {
    localSongsKeyBindings.forEach { b -> b.keys.forEach { k -> put(k, b.action) } }
}

fun mapLocalSongsKey(key: String): LocalSongsAction? = localSongsKeyMap[key]

// ── Dispatch ──

fun dispatchLocalSongsKey(
    event: KeyEvent,
    focus: LocalFocus,
    showPaths: Boolean,
    onSetShowPaths: (Boolean) -> Unit,
    onSetFocus: (LocalFocus) -> Unit,
    onGoBack: () -> Unit,
    onAddPath: () -> Unit,
    onRemovePath: () -> Unit,
    onRefresh: () -> Unit,
): Boolean = when (focus) {
    LocalFocus.Input -> false // InputField handles its own keys

    LocalFocus.Paths -> {
        val screenAction = mapLocalSongsKey(event.key)
        when (screenAction) {
            LocalSongsAction.DeletePath -> { onRemovePath(); true }
            LocalSongsAction.AddPath -> { onSetFocus(LocalFocus.Input); true }
            LocalSongsAction.TogglePaths -> { onSetShowPaths(false); onSetFocus(LocalFocus.Songs); true }
            LocalSongsAction.GoBack -> { onSetShowPaths(false); onSetFocus(LocalFocus.Songs); true }
            else -> false // ListView handles its own keys
        }
    }

    LocalFocus.Songs -> {
        val screenAction = mapLocalSongsKey(event.key)
        when (screenAction) {
            LocalSongsAction.Refresh -> { onRefresh(); true }
            LocalSongsAction.TogglePaths -> { onSetShowPaths(true); onSetFocus(LocalFocus.Paths); true }
            LocalSongsAction.AddPath -> { onSetShowPaths(true); onSetFocus(LocalFocus.Input); true }
            LocalSongsAction.GoBack -> { onGoBack(); true }
            else -> false // TrackTable handles its own keys
        }
    }
}

fun localSongsStatusBarHint(focus: LocalFocus): String = when (focus) {
    LocalFocus.Input -> inputFieldStatusBarHint()
    LocalFocus.Paths -> "j/k:Scroll  d:Delete  a:Add  /:Toggle  Esc:Back"
    LocalFocus.Songs -> "j/k:Scroll  Enter:Play  p:PlayAll  r:Refresh  /:ManagePaths  Esc:Back"
}

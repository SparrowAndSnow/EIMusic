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
    data object OpenMenu : LocalSongsAction
}

val localSongsKeyBindings: List<KeyBinding<LocalSongsAction>> = listOf(
    KeyBinding(listOf("r"), "r", LocalSongsAction.Refresh, "Refresh"),
    KeyBinding(listOf("/"), "/", LocalSongsAction.TogglePaths, "Manage paths"),
    KeyBinding(listOf("a"), "a", LocalSongsAction.AddPath, "Add path"),
    KeyBinding(listOf("d"), "d", LocalSongsAction.DeletePath, "Delete path"),
    KeyBinding(listOf("Escape"), "Esc", LocalSongsAction.GoBack, "Back"),
    KeyBinding(listOf("m"), "m", LocalSongsAction.OpenMenu, "Menu"),
)

private val localSongsKeyMap: Map<String, LocalSongsAction> = buildMap {
    localSongsKeyBindings.forEach { b -> b.keys.forEach { k -> put(k, b.action) } }
}

fun mapLocalSongsKey(key: String): LocalSongsAction? = localSongsKeyMap[key]

// ── Dispatch ──

/** Local 页面按键回调集合，避免 dispatch 函数参数继续膨胀。 */
data class LocalSongsActions(
    val setShowPaths: (Boolean) -> Unit,
    val setFocus: (LocalFocus) -> Unit,
    val goBack: () -> Unit,
    val addPath: () -> Unit,
    val removePath: () -> Unit,
    val refresh: () -> Unit,
    val openMenu: () -> Unit,
)

fun dispatchLocalSongsKey(
    event: KeyEvent,
    focus: LocalFocus,
    showPaths: Boolean,
    actions: LocalSongsActions,
): Boolean = when (focus) {
    LocalFocus.Input -> false // InputField handles its own keys

    LocalFocus.Paths -> {
        val screenAction = mapLocalSongsKey(event.key)
        when (screenAction) {
            LocalSongsAction.DeletePath -> { actions.removePath(); true }
            LocalSongsAction.AddPath -> { actions.setFocus(LocalFocus.Input); true }
            LocalSongsAction.TogglePaths -> { actions.setShowPaths(false); actions.setFocus(LocalFocus.Songs); true }
            LocalSongsAction.GoBack -> { actions.setShowPaths(false); actions.setFocus(LocalFocus.Songs); true }
            else -> false // ListView handles its own keys
        }
    }

    LocalFocus.Songs -> {
        val screenAction = mapLocalSongsKey(event.key)
        when (screenAction) {
            LocalSongsAction.Refresh -> { actions.refresh(); true }
            LocalSongsAction.TogglePaths -> { actions.setShowPaths(true); actions.setFocus(LocalFocus.Paths); true }
            LocalSongsAction.AddPath -> { actions.setShowPaths(true); actions.setFocus(LocalFocus.Input); true }
            LocalSongsAction.GoBack -> { actions.goBack(); true }
            LocalSongsAction.OpenMenu -> { actions.openMenu(); true }
            else -> false // TrackTable handles its own keys
        }
    }
}

fun localSongsStatusBarHint(focus: LocalFocus): String = when (focus) {
    LocalFocus.Input -> inputFieldStatusBarHint()
    LocalFocus.Paths -> "j/k:Scroll  d:Delete  a:Add  /:Toggle  Esc:Back"
    LocalFocus.Songs -> "j/k:Scroll  Enter:Play  p:PlayAll  r:Refresh  /:ManagePaths  m:Menu  Esc:Back"
}

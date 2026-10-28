package com.eimsound.eimusic.keyboard

import com.eimsound.eimusic.util.spacer
import com.jakewharton.mosaic.layout.KeyEvent

// ── Main screen actions ──
sealed interface MainAction : KeyAction {
    data object PlayPause : MainAction
    data object Next : MainAction
    data object Previous : MainAction
    data object TogglePlayMode : MainAction
    data object ToggleHelp : MainAction
    data object OpenSearch : MainAction
    data object OpenSettings : MainAction
    data object OpenLogin : MainAction
    data object OpenLogs : MainAction
    data object SeekForward : MainAction
    data object SeekBackward : MainAction
    data object VolumeUp : MainAction
    data object VolumeDown : MainAction
    data object ToggleTheme : MainAction
    data object Quit : MainAction
}

/**
 * A keybinding entry: one or more physical keys + a display label + description.
 */
/**
 * All main-screen keybindings listed in one place.
 * Used both for key dispatch and for rendering the help dialog.
 */
val mainKeyBindings: List<KeyBinding<MainAction>> = listOf(
    KeyBinding(listOf("?"), "?", MainAction.ToggleHelp, "Toggle this help"),
    KeyBinding(listOf(" "), "Space", MainAction.PlayPause, "Play / Pause"),
    KeyBinding(listOf("["), "[", MainAction.Previous, "Previous track"),
    KeyBinding(listOf("]"), "]", MainAction.Next, "Next track"),
    KeyBinding(listOf("m"), "m", MainAction.TogglePlayMode, "Toggle play mode"),
    KeyBinding(listOf("ArrowLeft"), "←  →", MainAction.SeekBackward, "Seek -5s"),
    KeyBinding(listOf("ArrowRight"), "←  →", MainAction.SeekForward, "Seek +5s"),
    KeyBinding(listOf("+", "="), "+  -", MainAction.VolumeUp, "Volume up"),
    KeyBinding(listOf("-", "_"), "+  -", MainAction.VolumeDown, "Volume down"),
    KeyBinding(listOf("t"), "t", MainAction.ToggleTheme, "Toggle theme"),
    KeyBinding(listOf("l"), "l", MainAction.OpenLogin, "Login"),
    KeyBinding(listOf("f"), "f", MainAction.OpenSearch, "Search music"),
    KeyBinding(listOf("s"), "s", MainAction.OpenSettings, "Settings"),
    KeyBinding(listOf("`"), "`", MainAction.OpenLogs, "View logs"),
    KeyBinding(listOf("q"), "q", MainAction.Quit, "Quit"),
)

/** Lookup map built from [mainKeyBindings] for O(1) dispatch. */
private val mainKeyMap: Map<String, MainAction> = buildMap {
    mainKeyBindings.forEach { binding ->
        binding.keys.forEach { key -> put(key, binding.action) }
    }
}

/**
 * Maps a key string to a [MainAction].
 */
fun mapMainKey(key: String): MainAction? = mainKeyMap[key]

/**
 * Maps a Ctrl+key combination to a [MainAction].
 */
private val ctrlKeyMap: Map<String, MainAction> = mapOf(
    "c" to MainAction.Quit,
)

fun mapCtrlKey(key: String): MainAction? = ctrlKeyMap[key]

/**
 * Help entries deduplicated by display label (for rendering).
 * Groups bindings that share the same display string.
 */
val mainHelpEntries: List<Pair<String, String>> = mainKeyBindings
    .distinctBy { it.display }
    .map { it.display to it.description }

/**
 * Status bar hint built from [mainKeyBindings], so it stays in sync.
 * Shows only the essential actions with short labels.
 */
fun mainStatusBarHint(): String {
    val show = setOf(
        MainAction.ToggleHelp,
        MainAction.PlayPause, MainAction.Previous, MainAction.Next,
        MainAction.OpenSearch,
        MainAction.Quit,
    )
    val labels: Map<MainAction, String> = mapOf(
        MainAction.ToggleHelp to "Help",
        MainAction.PlayPause to "Play/Pause",
        MainAction.Previous to "Prev",
        MainAction.Next to "Next",
        MainAction.OpenSearch to "Search",
        MainAction.Quit to "Quit",
    )
    return mainKeyBindings
        .filter { it.action in show }
        .distinctBy { it.display }
        .joinToString(2.spacer) { "${it.display}:${labels[it.action] ?: ""}" }
}

/**
 * Central key event dispatcher for the main screen.
 */
fun dispatchMainKey(
    event: KeyEvent,
    globalActions: GlobalKeyActions,
    onToggleHelp: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenLogs: () -> Unit,
    onToggleTheme: () -> Unit,
    onQuit: () -> Unit,
): Boolean {
    val action = if (event.ctrl) mapCtrlKey(event.key) else mapMainKey(event.key)
    if (action == null) return false

    when (action) {
        MainAction.PlayPause -> globalActions.onPlayPause()
        MainAction.Next -> globalActions.onNext()
        MainAction.Previous -> globalActions.onPrevious()
        MainAction.TogglePlayMode -> globalActions.onTogglePlayMode()
        MainAction.ToggleHelp -> onToggleHelp()
        MainAction.OpenSearch -> onOpenSearch()
        MainAction.OpenSettings -> onOpenSettings()
        MainAction.OpenLogin -> onOpenLogin()
        MainAction.OpenLogs -> onOpenLogs()
        MainAction.SeekForward -> globalActions.onSeekForward()
        MainAction.SeekBackward -> globalActions.onSeekBackward()
        MainAction.VolumeUp -> globalActions.onVolumeUp()
        MainAction.VolumeDown -> globalActions.onVolumeDown()
        MainAction.ToggleTheme -> onToggleTheme()
        MainAction.Quit -> onQuit()
    }
    return true
}

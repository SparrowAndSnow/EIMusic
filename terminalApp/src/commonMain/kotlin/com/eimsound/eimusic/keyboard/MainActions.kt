package com.eimsound.eimusic.keyboard

import com.eimsound.eimusic.i18n.t
import com.eimsound.eimusic.util.spacer

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
        MainAction.ToggleHelp to t("hint.main.help"),
        MainAction.PlayPause to t("hint.main.playPause"),
        MainAction.Previous to t("hint.main.prev"),
        MainAction.Next to t("hint.main.next"),
        MainAction.OpenSearch to t("hint.main.search"),
        MainAction.Quit to t("hint.main.quit"),
    )
    return mainKeyBindings
        .filter { it.action in show }
        .distinctBy { it.display }
        .joinToString(2.spacer) { "${it.display}:${labels[it.action] ?: ""}" }
}


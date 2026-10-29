package com.eimsound.eimusic.keyboard

import com.eimsound.eimusic.i18n.t
import com.jakewharton.mosaic.layout.KeyEvent

// ── Focus ──

enum class SettingFocus { Main, Sources, Editing }

// ── Screen-specific actions ──

sealed interface SettingAction : KeyAction {
    data object MoveDown : SettingAction
    data object MoveUp : SettingAction
    data object Confirm : SettingAction
    data object Back : SettingAction
}

val settingKeyBindings: List<KeyBinding<SettingAction>> = listOf(
    KeyBinding(listOf("j", "ArrowDown"), "j/k", SettingAction.MoveDown, "Move down"),
    KeyBinding(listOf("k", "ArrowUp"), "j/k", SettingAction.MoveUp, "Move up"),
    KeyBinding(listOf("Enter"), "Enter", SettingAction.Confirm, "Open / edit"),
    KeyBinding(listOf("Escape"), "Esc", SettingAction.Back, "Back"),
)

private val settingKeyMap: Map<String, SettingAction> = buildMap {
    settingKeyBindings.forEach { binding ->
        binding.keys.forEach { key -> put(key, binding.action) }
    }
}

fun mapSettingKey(key: String): SettingAction? = settingKeyMap[key]

// ── Dispatch ──

/** 设置页按键回调；列表页和音源子页复用。 */
data class SettingKeyActions(
    val moveSelection: (delta: Int) -> Unit,
    val confirm: () -> Unit,
    val back: () -> Unit,
)

fun dispatchSettingKey(
    event: KeyEvent,
    focus: SettingFocus,
    actions: SettingKeyActions,
): Boolean = when (focus) {
    SettingFocus.Editing -> false // InputField handles Enter/Escape itself
    SettingFocus.Main, SettingFocus.Sources -> when (mapSettingKey(event.key)) {
        SettingAction.MoveDown -> { actions.moveSelection(1); true }
        SettingAction.MoveUp -> { actions.moveSelection(-1); true }
        SettingAction.Confirm -> { actions.confirm(); true }
        SettingAction.Back -> { actions.back(); true }
        null -> false
    }
}

fun settingStatusBarHint(focus: SettingFocus): String = when (focus) {
    SettingFocus.Main -> t("hint.settings.main")
    SettingFocus.Sources -> t("hint.settings.sources")
    SettingFocus.Editing -> t("hint.settings.editing")
}

package com.eimsound.eimusic.keyboard

import com.eimsound.eimusic.i18n.t

sealed interface InputFieldBindingAction : KeyAction {
    data object Confirm : InputFieldBindingAction
    data object Cancel : InputFieldBindingAction
    data object DeleteChar : InputFieldBindingAction
    data object DeleteForward : InputFieldBindingAction
    data object MoveCursorLeft : InputFieldBindingAction
    data object MoveCursorRight : InputFieldBindingAction
    data object MoveCursorUp : InputFieldBindingAction
    data object MoveCursorDown : InputFieldBindingAction
}

/**
 * 输入框在帮助/状态栏里展示的按键说明。
 *
 * 实际编辑行为由 Mosaic 的 BasicTextField 负责（字素簇光标、IME、粘贴）；
 * 这里只保留面向用户的绑定描述，不再自己分发按键。
 */
val inputFieldKeyBindings: List<KeyBinding<InputFieldBindingAction>> = listOf(
    KeyBinding(listOf("Enter"), "Enter", InputFieldBindingAction.Confirm, "Confirm"),
    KeyBinding(listOf("Escape"), "Esc", InputFieldBindingAction.Cancel, "Cancel"),
    KeyBinding(listOf("Backspace"), "⌫", InputFieldBindingAction.DeleteChar, "Delete"),
    KeyBinding(listOf("Delete"), "Del", InputFieldBindingAction.DeleteForward, "Delete forward"),
    KeyBinding(listOf("ArrowLeft"), "←", InputFieldBindingAction.MoveCursorLeft, "Cursor left"),
    KeyBinding(listOf("ArrowRight"), "→", InputFieldBindingAction.MoveCursorRight, "Cursor right"),
    KeyBinding(listOf("ArrowUp"), "↑", InputFieldBindingAction.MoveCursorUp, "Cursor up"),
    KeyBinding(listOf("ArrowDown"), "↓", InputFieldBindingAction.MoveCursorDown, "Cursor down"),
)

fun inputFieldStatusBarHint(): String = t("hint.input")

val inputFieldHelpEntries: List<Pair<String, String>> = inputFieldKeyBindings
    .distinctBy { it.display }
    .map { it.display to it.description }

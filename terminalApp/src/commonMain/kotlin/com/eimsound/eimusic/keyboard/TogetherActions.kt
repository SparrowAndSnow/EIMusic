package com.eimsound.eimusic.keyboard

import com.jakewharton.mosaic.layout.KeyEvent

// ── Focus ──

enum class TogetherFocus { Welcome, Messages, ChatInput, ChannelIdInput }

// ── Screen-specific actions ──

sealed interface TogetherAction : KeyAction {
    data object JoinChannel : TogetherAction
    data object LeaveChannel : TogetherAction
    data object ToggleChat : TogetherAction
    data object BackToWelcome : TogetherAction
    data object Disconnect : TogetherAction
}

val togetherKeyBindings: List<KeyBinding<TogetherAction>> = listOf(
    KeyBinding(listOf("j"), "j", TogetherAction.JoinChannel, "Join"),
    KeyBinding(listOf("l"), "l", TogetherAction.LeaveChannel, "Leave"),
    KeyBinding(listOf("m"), "m", TogetherAction.ToggleChat, "Chat"),
    KeyBinding(listOf("Escape"), "Esc", TogetherAction.BackToWelcome, "Back"),
)

private val togetherKeyMap: Map<String, TogetherAction> = buildMap {
    togetherKeyBindings.forEach { b -> b.keys.forEach { k -> put(k, b.action) } }
}

fun mapTogetherKey(key: String): TogetherAction? = togetherKeyMap[key]

// ── Dispatch ──

fun dispatchTogetherKey(
    event: KeyEvent,
    focus: TogetherFocus,
    isJoined: Boolean,
    onLeaveChannel: () -> Unit,
    onSetFocus: (TogetherFocus) -> Unit,
    onGoBack: () -> Unit,
): Boolean = when (focus) {
    TogetherFocus.Welcome -> when (mapTogetherKey(event.key)) {
        TogetherAction.JoinChannel -> { onSetFocus(TogetherFocus.Messages); true }
        TogetherAction.LeaveChannel -> { onLeaveChannel(); true }
        TogetherAction.ToggleChat -> false
        TogetherAction.Disconnect,
        TogetherAction.BackToWelcome -> { onGoBack(); true }
        null -> false
    }

    TogetherFocus.Messages -> {
        when (mapTogetherKey(event.key)) {
            TogetherAction.ToggleChat -> { if (isJoined) { onSetFocus(TogetherFocus.ChatInput); true } else false }
            TogetherAction.LeaveChannel -> { onLeaveChannel(); onSetFocus(TogetherFocus.Welcome); true }
            TogetherAction.BackToWelcome -> { onSetFocus(TogetherFocus.Welcome); true }
            else -> false // ListView handles its own keys
        }
    }

    TogetherFocus.ChatInput -> false // InputField handles its own keys

    TogetherFocus.ChannelIdInput -> false // InputField handles its own keys
}

fun togetherStatusBarHint(focus: TogetherFocus): String = when (focus) {
    TogetherFocus.Welcome -> "j:Join  l:Leave  Esc:Back"
    TogetherFocus.Messages -> "j/k:Scroll  m:Chat  l:Leave  Esc:Back"
    TogetherFocus.ChatInput -> "Enter:Send  Esc:Cancel"
    TogetherFocus.ChannelIdInput -> "Enter:Join  Esc:Cancel"
}

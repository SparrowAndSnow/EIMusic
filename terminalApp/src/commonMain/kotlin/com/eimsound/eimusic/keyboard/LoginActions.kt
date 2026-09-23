package com.eimsound.eimusic.keyboard

import com.jakewharton.mosaic.layout.KeyEvent


sealed interface LoginAction : KeyAction {
    data object Close : LoginAction
}

val loginKeyBindings: List<KeyBinding<LoginAction>> = listOf(
    KeyBinding(listOf("Escape", "l"), "Esc", LoginAction.Close, "Close"),
)

private val loginKeyMap: Map<String, LoginAction> = buildMap {
    loginKeyBindings.forEach { b -> b.keys.forEach { k -> put(k, b.action) } }
}

fun mapLoginKey(key: String): LoginAction? = loginKeyMap[key]

fun dispatchLoginKey(
    event: KeyEvent,
    onClose: () -> Unit,
): Boolean {
    val screenAction = mapLoginKey(event.key)
    return when (screenAction) {
        LoginAction.Close -> { onClose(); true }
        null -> false
    }
}
fun loginStatusBarHint(): String = inputFieldStatusBarHint()

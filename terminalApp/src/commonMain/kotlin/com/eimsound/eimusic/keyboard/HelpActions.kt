package com.eimsound.eimusic.keyboard

// ── Help overlay actions ──
sealed interface HelpAction : KeyAction {
    data object Dismiss : HelpAction
}

fun mapHelpKey(key: String): HelpAction? = when (key) {
    "Escape", "?" -> HelpAction.Dismiss
    else -> null
}

fun helpStatusBarHint(): String = "Esc:Close"

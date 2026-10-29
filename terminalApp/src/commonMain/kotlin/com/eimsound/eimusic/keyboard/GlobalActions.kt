package com.eimsound.eimusic.keyboard

import androidx.compose.runtime.staticCompositionLocalOf
import com.jakewharton.mosaic.layout.KeyEvent

sealed interface KeyAction

/**
 * A keybinding entry: one or more physical keys + a display label + description.
 * The [T] type parameter is the action type (e.g., [MainAction]).
 */
data class KeyBinding<T>(
    val keys: List<String>,
    val display: String,
    val action: T,
    val description: String,
)

/**
 * 全局播放控制按键动作集合。
 * 不依赖 ViewModel，由调用方把 ViewModel 方法包成 lambda。
 */
class GlobalKeyActions(
    val onPlayPause: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onVolumeUp: () -> Unit = {},
    val onVolumeDown: () -> Unit = {},
    val onVolumeScroll: (Int) -> Unit = {},
    val onSeekForward: () -> Unit = {},
    val onSeekBackward: () -> Unit = {},
    val onTogglePlayMode: () -> Unit = {},
    val onToggleMute: () -> Unit = {},
    val onOpenSearch: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onToggleHelp: () -> Unit = {},
    val onOpenLogin: () -> Unit = {},
    val onOpenLogs: () -> Unit = {},
    val onToggleTheme: () -> Unit = {},
    val onQuit: () -> Unit = {},
)

/** 通过 CompositionLocal 提供，避免层层传参。在 App.kt 顶层创建一次即可。 */
val LocalGlobalActions = staticCompositionLocalOf<GlobalKeyActions> {
    error("GlobalKeyActions not provided. Wrap your content in CompositionLocalProvider.")
}

/** 处理全局播放控制按键，返回 true 表示已消费 */
fun dispatchGlobalKey(event: KeyEvent, actions: GlobalKeyActions): Boolean {
    val action = mapMainKey(event.key) ?: return false
    return when (action) {
        MainAction.PlayPause -> { actions.onPlayPause(); true }
        MainAction.Next -> { actions.onNext(); true }
        MainAction.Previous -> { actions.onPrevious(); true }
        MainAction.TogglePlayMode -> { actions.onTogglePlayMode(); true }
        MainAction.SeekForward -> { actions.onSeekForward(); true }
        MainAction.SeekBackward -> { actions.onSeekBackward(); true }
        MainAction.VolumeUp -> { actions.onVolumeUp(); true }
        MainAction.VolumeDown -> { actions.onVolumeDown(); true }
        MainAction.OpenSearch -> { actions.onOpenSearch(); true }
        MainAction.OpenSettings -> { actions.onOpenSettings(); true }
        MainAction.ToggleHelp -> { actions.onToggleHelp(); true }
        MainAction.OpenLogin -> { actions.onOpenLogin(); true }
        MainAction.OpenLogs -> { actions.onOpenLogs(); true }
        MainAction.ToggleTheme -> { actions.onToggleTheme(); true }
        MainAction.Quit -> { actions.onQuit(); true }
        else -> false
    }
}

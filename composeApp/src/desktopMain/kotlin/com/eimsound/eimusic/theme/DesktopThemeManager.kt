package com.eimsound.eimusic.theme

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.jetbrains.skiko.currentSystemTheme
import org.jetbrains.skiko.SystemTheme as SkikoSystemTheme

/**
 * 桌面平台主题管理器，用于监听和跟踪系统主题变化
 */
object DesktopThemeManager {
    private var _isSystemInDarkTheme = mutableStateOf(currentSystemTheme == SkikoSystemTheme.DARK)
    val isSystemInDarkTheme: MutableState<Boolean> get() = _isSystemInDarkTheme
    
    /**
     * 当系统主题发生变化时调用此方法更新状态
     */
    fun updateSystemTheme(isDark: Boolean) {
        _isSystemInDarkTheme.value = isDark
    }
}
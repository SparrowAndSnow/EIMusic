package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

/** App 层包一个 CompositionLocalProvider，各 Screen 通过此状态设置 StatusBar 提示。 */
val LocalStatusBarHint = compositionLocalOf { mutableStateOf("") }

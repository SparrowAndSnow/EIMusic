package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.ui.uiKeyEvent
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.BoxScope

/**
 * 页面骨架：统一处理输入权限、按键分发和状态栏提示。
 *
 * 底层页面被弹窗冻结时（[LocalUiInputEnabled] 为 false），这里不会写 hint，
 * 避免和弹窗的 hint 抢状态；恢复后由 [LaunchedEffect] 重新写入。
 */
@Composable
fun ScreenFrame(
    hint: String,
    onKey: (KeyEvent) -> Boolean,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val inputEnabled = LocalUiInputEnabled.current
    val statusBarHint = LocalStatusBarHint.current
    LaunchedEffect(inputEnabled, hint) {
        if (inputEnabled) {
            statusBarHint.value = hint
        }
    }
    Box(
        modifier = modifier.fillMaxSize().uiKeyEvent(onKey),
        contentAlignment = contentAlignment,
        content = content,
    )
}

package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.ColumnScope

/**
 * 居中弹窗页面：ScreenFrame + Dialog 的固定组合。
 * 新增弹窗页面时只需要提供标题、hint、按键处理和内容。
 */
@Composable
fun DialogScreen(
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    onKey: (KeyEvent) -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ScreenFrame(
        hint = hint,
        onKey = onKey,
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Dialog(title = title, onDismiss = onDismiss, content = content)
    }
}

package com.eimsound.eimusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier

/**
 * false 时该子树不参与键盘/鼠标输入。
 *
 * 弹窗叠在页面上时，底层页面仍需要绘制，但必须冻结输入，
 * 否则按键会先被底层的列表/输入框消费掉。
 */
val LocalUiInputEnabled = staticCompositionLocalOf { true }

/** 只在 [LocalUiInputEnabled] 为 true 时处理按键。 */
@Composable
fun Modifier.uiKeyEvent(handler: (KeyEvent) -> Boolean): Modifier {
    val enabled = LocalUiInputEnabled.current
    return onKeyEvent { event -> enabled && handler(event) }
}

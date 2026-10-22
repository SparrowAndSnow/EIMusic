package com.eimsound.eimusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * 页面叠层：先绘制 base（冻结输入），再绘制 overlay（唯一可交互层）。
 * 弹层页面不需要感知自己是不是 overlay。
 */
@Composable
fun OverlayHost(
    base: (@Composable () -> Unit)?,
    overlay: @Composable () -> Unit,
) {
    if (base != null) {
        CompositionLocalProvider(LocalUiInputEnabled provides false) {
            base()
        }
    }
    CompositionLocalProvider(LocalUiInputEnabled provides true) {
        overlay()
    }
}

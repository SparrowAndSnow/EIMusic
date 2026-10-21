package com.eimsound.eimusic.ui.theme

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 内容区可用尺寸，由 App 的 BoxWithConstraints 按真实父级约束提供
 * （已扣掉 HeaderBar 与 StatusBar），不再用终端行列数手工硬减。
 *
 * [rows] 是内容区高度；各屏幕仍应按自己的标题/表头再扣减。
 * 计算可见行数时始终对结果 [coerceAtLeast]。
 */
data class SafeContentSize(
    val columns: Int,
    val rows: Int,
)

val LocalSafeContentSize: ProvidableCompositionLocal<SafeContentSize> =
    staticCompositionLocalOf { SafeContentSize(80, 10) }

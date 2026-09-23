package com.eimsound.eimusic.ui.theme

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Safe content area dimensions, accounting for HeaderBar, StatusBar,
 * screen titles, column headers, etc.
 *
 * [rows] is the number of terminal rows available for ListView content.
 * Screens should further subtract their own overhead (titles, headers).
 * Always clamp visibleLines to [coerceAtLeast(1)].
 */
data class SafeContentSize(
    val columns: Int,
    val rows: Int,
)

val LocalSafeContentSize: ProvidableCompositionLocal<SafeContentSize> =
    staticCompositionLocalOf { SafeContentSize(80, 10) }

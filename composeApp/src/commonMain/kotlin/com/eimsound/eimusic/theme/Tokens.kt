package com.eimsound.eimusic.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class EIMusicPalette(
    val accent: Color,
    val accentMuted: Color,
    val surfaceBase: Color,
    val surfaceRaised: Color,
    val surfaceGlass: Color,
    val borderSoft: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,
    val error: Color,
)

@Immutable
data class EIMusicSpacing(
    val xs: Int = 4,
    val sm: Int = 8,
    val md: Int = 12,
    val lg: Int = 16,
    val xl: Int = 24,
    val xxl: Int = 32,
)

object EIMusicGradients {
    fun appBackground(isDark: Boolean): Brush {
        return if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0A0C0E),
                    Color(0xFF12171B),
                    Color(0xFF0C1015),
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF4F7F5),
                    Color(0xFFEAF0EC),
                    Color(0xFFF5F8F6),
                )
            )
        }
    }
}

val DarkPalette = EIMusicPalette(
    accent = Color(0xFF00CED1),
    accentMuted = Color(0xFF48D1CC),
    surfaceBase = Color(0xFF0B0F13),
    surfaceRaised = Color(0xFF141A21),
    surfaceGlass = Color(0xCC1B222B),
    borderSoft = Color(0xFF263241),
    borderStrong = Color(0xFF35516C),
    textPrimary = Color(0xFFF2F6F9),
    textSecondary = Color(0xFF9BB0C4),
    success = Color(0xFF3E7A5B),
    error = Color(0xFFFF6B6B)
)

val LightPalette = EIMusicPalette(
    accent = Color(0xFF008B8B),
    accentMuted = Color(0xFF00CED1),
    surfaceBase = Color(0xFFF2F6F4),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceGlass = Color(0xCCFFFFFF),
    borderSoft = Color(0xFFD5DFD8),
    borderStrong = Color(0xFFB5C9BB),
    textPrimary = Color(0xFF0D1A15),
    textSecondary = Color(0xFF4D6357),
    success = Color(0xFF3E7A5B),
    error = Color(0xFFBA1A1A)
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }
val LocalSpacing = staticCompositionLocalOf { EIMusicSpacing() }

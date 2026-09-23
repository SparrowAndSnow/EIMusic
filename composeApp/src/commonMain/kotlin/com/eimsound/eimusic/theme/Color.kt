package com.eimsound.eimusic.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    primary = DarkPalette.accent,
    onPrimary = Color(0xFF03140A),
    primaryContainer = Color(0xFF0F3A20),
    onPrimaryContainer = Color(0xFFC8FFD9),
    secondary = Color(0xFF80A9BE),
    onSecondary = Color(0xFF0C1B25),
    secondaryContainer = Color(0xFF1A2A35),
    onSecondaryContainer = Color(0xFFCEE9F9),
    tertiary = Color(0xFF88C8AA),
    onTertiary = Color(0xFF0A2217),
    tertiaryContainer = Color(0xFF163628),
    onTertiaryContainer = Color(0xFFBEF5D9),
    background = DarkPalette.surfaceBase,
    onBackground = DarkPalette.textPrimary,
    surface = DarkPalette.surfaceBase,
    onSurface = DarkPalette.textPrimary,
    surfaceVariant = DarkPalette.surfaceRaised,
    onSurfaceVariant = DarkPalette.textSecondary,
    outline = DarkPalette.borderSoft,
    outlineVariant = DarkPalette.borderStrong,
    error = DarkPalette.error,
    onError = Color(0xFF380404),
    errorContainer = Color(0xFF5F1515),
    onErrorContainer = Color(0xFFFFDAD8),
)

val LightColorScheme = lightColorScheme(
    primary = LightPalette.accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6F5D4),
    onPrimaryContainer = Color(0xFF03210E),
    secondary = Color(0xFF315E73),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E9F7),
    onSecondaryContainer = Color(0xFF001E2D),
    tertiary = Color(0xFF3E7A5B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC1EED4),
    onTertiaryContainer = Color(0xFF002111),
    background = LightPalette.surfaceBase,
    onBackground = LightPalette.textPrimary,
    surface = LightPalette.surfaceBase,
    onSurface = LightPalette.textPrimary,
    surfaceVariant = LightPalette.surfaceRaised,
    onSurfaceVariant = LightPalette.textSecondary,
    outline = LightPalette.borderSoft,
    outlineVariant = LightPalette.borderStrong,
    error = LightPalette.error,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

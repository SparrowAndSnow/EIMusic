package com.eimsound.eimusic.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun colorScheme(darkTheme: Boolean): ColorScheme = when {
    darkTheme -> darkScheme
    else -> lightScheme
}

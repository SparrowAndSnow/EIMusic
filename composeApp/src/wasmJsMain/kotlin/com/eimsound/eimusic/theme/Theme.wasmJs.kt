package com.eimsound.eimusic.theme

import androidx.compose.material3.ColorScheme

actual fun colorScheme(darkTheme: Boolean): ColorScheme = when {
    darkTheme -> darkScheme
    else -> lightScheme
}
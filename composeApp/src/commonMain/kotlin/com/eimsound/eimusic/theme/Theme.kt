package com.eimsound.eimusic.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.Modifier
import com.eimsound.eimusic.util.Toast
import com.eimsound.eimusic.util.LocalToastMessage

@Composable
fun EIMusicTheme(
    content: @Composable () -> Unit
) {
    val isDark = Theme.current
    val color = if (isDark) DarkColorScheme else LightColorScheme
    val palette = if (isDark) DarkPalette else LightPalette
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalMotion provides EIMusicMotion(),
        LocalSpacing provides EIMusicSpacing(),
        LocalRadii provides EIMusicRadii(),
        LocalToastMessage provides Toast,
    ) {
        MaterialTheme(
            colorScheme = color,
            typography = typography(),
            shapes = appShapes,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(EIMusicGradients.appBackground(isDark))
            ) {
                content()
            }
        }
    }
}

expect object Theme {
    val current: Boolean @Composable get

    val isSystemInDarkTheme: Boolean @Composable get

    @Composable
    infix fun provides(value: Boolean?): ProvidedValue<*>
}

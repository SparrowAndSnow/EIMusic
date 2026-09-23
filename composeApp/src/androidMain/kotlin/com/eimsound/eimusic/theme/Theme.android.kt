package com.eimsound.eimusic.theme

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@OptIn(InternalComposeUiApi::class)
actual object Theme {
    actual val current: Boolean
        @Composable get() = (LocalConfiguration.current.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES

    @Composable
    actual infix fun provides(value: Boolean?): ProvidedValue<*> {
        val new = if (value == null) {
            LocalConfiguration.current
        } else {
            val view = LocalView.current
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !value
            controller.isAppearanceLightNavigationBars = !value

            Configuration(LocalConfiguration.current).apply {
                uiMode = when (value) {
                    true -> (uiMode and UI_MODE_NIGHT_MASK.inv()) or UI_MODE_NIGHT_YES
                    false -> (uiMode and UI_MODE_NIGHT_MASK.inv()) or UI_MODE_NIGHT_NO
                }
            }
        }
        return LocalConfiguration.provides(new)
    }

    actual val isSystemInDarkTheme: Boolean
        @Composable get() = isSystemInDarkTheme()
}
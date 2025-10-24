package com.eimsound.eimusic.theme

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.LocalConfiguration


//@Composable
//actual fun colorScheme(darkTheme: Boolean): ColorScheme = when {
//    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//        val context = LocalContext.current
//        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//    }
//
//    darkTheme -> darkScheme
//    else -> lightScheme
//}

@OptIn(InternalComposeUiApi::class)
actual object Theme {
    actual val current: Boolean
        @Composable get() = (LocalConfiguration.current.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES

    @Composable
    actual infix fun provides(value: Boolean?): ProvidedValue<*> {
        val new = if (value == null) {
            LocalConfiguration.current
        } else {
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
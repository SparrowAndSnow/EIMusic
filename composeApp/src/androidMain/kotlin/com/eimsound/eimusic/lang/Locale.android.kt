package com.eimsound.eimusic.lang

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat

actual object Locale {
    actual val current: String
        @Composable get() = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(value)
        AppCompatDelegate.setApplicationLocales(appLocale)
        return LocalConfiguration.provides(configuration)
    }
}
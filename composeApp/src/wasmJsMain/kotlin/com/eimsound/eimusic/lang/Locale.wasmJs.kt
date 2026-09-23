package com.eimsound.eimusic.lang

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf

external object window {
    var __customLocale: String?
}

actual object Locale {
    private val LocalAppLocale = staticCompositionLocalOf { window.__customLocale }
    actual val current: String
        @Composable get() = LocalAppLocale.current.toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        window.__customLocale = value?.replace('_', '-')
        return LocalAppLocale.provides(Locale.current)
    }
}
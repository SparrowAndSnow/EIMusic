package com.eimsound.eimusic.lang

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object Locale {
    private var default: Locale? = null
    private val locale = staticCompositionLocalOf { Locale.getDefault().toString() }


    actual val current: String
        @Composable get() = locale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (default == null) {
            default = Locale.getDefault()
        }
        val new = when (value) {
            null -> default
            else -> Locale.of(value)
        }
        Locale.setDefault(new)
        return locale.provides(new.toString())
    }
}
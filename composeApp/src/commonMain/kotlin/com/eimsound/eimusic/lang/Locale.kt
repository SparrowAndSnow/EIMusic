package com.eimsound.eimusic.lang

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

expect object Locale {
    val current: String @Composable get
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}


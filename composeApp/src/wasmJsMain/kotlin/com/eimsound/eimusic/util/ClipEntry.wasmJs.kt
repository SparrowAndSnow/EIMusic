package com.eimsound.eimusic.util

import androidx.compose.ui.platform.ClipEntry

actual fun clipEntryOf(string: String): ClipEntry {
    return ClipEntry.withPlainText(string)
}
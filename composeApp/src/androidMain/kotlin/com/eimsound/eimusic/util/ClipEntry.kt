package com.eimsound.eimusic.util

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun clipEntryOf(string: String): ClipEntry {
    return ClipEntry(ClipData.newPlainText("text", string))
}
package com.eimsound.eimusic.util

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker

actual suspend fun openDirectoryPicker(): PlatformFile? {
    return FileKit.openDirectoryPicker()
}
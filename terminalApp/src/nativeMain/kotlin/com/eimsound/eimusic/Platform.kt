package com.eimsound.eimusic

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.OsFamily
import kotlin.native.Platform

/** Platform-specific default config directory path. */
expect fun defaultConfigDir(): String

/** Platform-specific cache directory for audio playback. */
expect fun defaultCacheDir(): String

@OptIn(ExperimentalNativeApi::class)
actual fun currentOsName(): String = when (Platform.osFamily) {
    OsFamily.LINUX -> "linux"
    OsFamily.WINDOWS -> "windows"
    OsFamily.MACOSX -> "osx"
    else -> "unknown"
}

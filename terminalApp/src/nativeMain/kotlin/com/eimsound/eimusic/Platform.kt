package com.eimsound.eimusic

/** Platform-specific default config directory path. */
expect fun defaultConfigDir(): String

/** Platform-specific cache directory for audio playback. */
expect fun defaultCacheDir(): String

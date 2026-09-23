package com.eimsound.eimusic

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun defaultConfigDir(): String {
    val home = getenv("HOME")?.toKString() ?: "/tmp"
    return "$home/.config/eimusic"
}

actual fun defaultCacheDir(): String = "/tmp/eimusic_play"

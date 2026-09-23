package com.eimsound.eimusic

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.windows.GetEnvironmentVariableA

@OptIn(ExperimentalForeignApi::class)
actual fun defaultConfigDir(): String {
    val appdata = getEnvVar("APPDATA") ?: "C:\\Users\\Default\\AppData\\Roaming"
    return "$appdata\\eimusic"
}

@OptIn(ExperimentalForeignApi::class)
actual fun defaultCacheDir(): String {
    val temp = getEnvVar("TEMP") ?: "C:\\Windows\\Temp"
    return "$temp\\eimusic_play"
}

@OptIn(ExperimentalForeignApi::class)
fun getEnvVar(name: String): String? = memScoped {
    val size = GetEnvironmentVariableA(name, null, 0u)
    if (size == 0u) return@memScoped null
    val buf = allocArray<ByteVar>(size.toInt())
    val resultSize = GetEnvironmentVariableA(name, buf, size)
    if (resultSize == 0u || resultSize > size) return@memScoped null
    buf.toKString()
}

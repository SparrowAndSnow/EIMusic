package com.eimsound.eimusic

interface Platform {
    val name: String
    val type: PlatformType
}

enum class PlatformType {
    ANDROID, DESKTOP, IOS, WASM, Native
}

expect fun getPlatform(): Platform
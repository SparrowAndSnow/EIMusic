package com.eimsound.eimusic

class NativePlatform(override val type: PlatformType = PlatformType.Native) : Platform {
    override val name: String = "Native"
}

actual fun getPlatform(): Platform = NativePlatform()
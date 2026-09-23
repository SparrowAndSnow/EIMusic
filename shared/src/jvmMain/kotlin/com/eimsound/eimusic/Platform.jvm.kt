package com.eimsound.eimusic

class JVMPlatform(override val type: PlatformType = PlatformType.DESKTOP) : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
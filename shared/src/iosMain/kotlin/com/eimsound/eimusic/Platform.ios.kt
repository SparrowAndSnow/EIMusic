package com.eimsound.eimusic

import platform.UIKit.UIDevice

class IOSPlatform(override val type: PlatformType = PlatformType.IOS) : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()
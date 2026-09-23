package com.eimsound.eimusic

import android.os.Build

class AndroidPlatform(override val type: PlatformType = PlatformType.ANDROID) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
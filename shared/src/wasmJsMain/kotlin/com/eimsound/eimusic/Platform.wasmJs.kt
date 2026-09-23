package com.eimsound.eimusic

class WasmPlatform(override val type: PlatformType = PlatformType.WASM) : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()
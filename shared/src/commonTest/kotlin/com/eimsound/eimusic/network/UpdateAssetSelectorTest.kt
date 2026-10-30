package com.eimsound.eimusic.network

import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.version.GitHubAsset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateAssetSelectorTest {
    private val assets = listOf(
        GitHubAsset("EIMusic-1.0.0.apk", "https://example.test/EIMusic-1.0.0.apk"),
        GitHubAsset("server-all.jar", "https://example.test/server-all.jar"),
        GitHubAsset("eimusic.kexe", "https://example.test/eimusic.kexe"),
        GitHubAsset("eimusic.exe", "https://example.test/eimusic.exe"),
    )

    @Test
    fun nativeLinuxSelectsKexeNotFirstAsset() {
        assertEquals("eimusic.kexe", selectUpdateAsset(assets, PlatformType.Native, "linux")?.name)
    }

    @Test
    fun nativeWindowsSelectsExe() {
        assertEquals("eimusic.exe", selectUpdateAsset(assets, PlatformType.Native, "windows")?.name)
    }

    @Test
    fun nativeMacHasNoReleaseAssetYet() {
        assertNull(selectUpdateAsset(assets, PlatformType.Native, "osx"))
    }

    @Test
    fun androidSelectsApk() {
        assertEquals("EIMusic-1.0.0.apk", selectUpdateAsset(assets, PlatformType.ANDROID, "linux")?.name)
    }

    @Test
    fun iosSelectsIpa() {
        val iosAssets = assets + GitHubAsset("EIMusic-1.0.0.ipa", "https://example.test/EIMusic-1.0.0.ipa")
        assertEquals("EIMusic-1.0.0.ipa", selectUpdateAsset(iosAssets, PlatformType.IOS, "ios")?.name)
    }

    @Test
    fun desktopAndWasmDoNotSelectArbitraryAsset() {
        assertNull(selectUpdateAsset(assets, PlatformType.DESKTOP, "linux"))
        assertNull(selectUpdateAsset(assets, PlatformType.WASM, "wasm"))
    }

    @Test
    fun unknownNativeOsFallsBackToReleasePage() {
        assertNull(selectUpdateAsset(assets, PlatformType.Native, "unknown"))
    }
}

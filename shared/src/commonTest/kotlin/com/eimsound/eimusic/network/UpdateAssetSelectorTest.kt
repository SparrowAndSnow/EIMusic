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
        GitHubAsset("eimusic-macos-arm64.kexe", "https://example.test/eimusic-macos-arm64.kexe"),
        GitHubAsset("eimusic-linux-x64.kexe", "https://example.test/eimusic-linux-x64.kexe"),
        GitHubAsset("eimusic-windows-x64.exe", "https://example.test/eimusic-windows-x64.exe"),
    )

    @Test
    fun nativeLinuxSelectsLinuxAssetWhenMacAssetComesFirst() {
        assertEquals("eimusic-linux-x64.kexe", selectUpdateAsset(assets, PlatformType.Native, "linux")?.name)
    }

    @Test
    fun nativeWindowsSelectsExe() {
        assertEquals("eimusic-windows-x64.exe", selectUpdateAsset(assets, PlatformType.Native, "windows")?.name)
    }

    @Test
    fun nativeMacSelectsMacAsset() {
        assertEquals("eimusic-macos-arm64.kexe", selectUpdateAsset(assets, PlatformType.Native, "macos")?.name)
        assertEquals("eimusic-macos-arm64.kexe", selectUpdateAsset(assets, PlatformType.Native, "osx")?.name)
    }

    @Test
    fun nativeMacFallsBackWhenReleaseHasNoMacAsset() {
        assertNull(selectUpdateAsset(assets.filterNot { it.name == "eimusic-macos-arm64.kexe" }, PlatformType.Native, "osx"))
    }

    @Test
    fun nativeLinuxAndWindowsAcceptLegacyAssetNames() {
        val legacyAssets = listOf(
            GitHubAsset("eimusic.kexe", "https://example.test/eimusic.kexe"),
            GitHubAsset("eimusic.exe", "https://example.test/eimusic.exe"),
        )
        assertEquals("eimusic.kexe", selectUpdateAsset(legacyAssets, PlatformType.Native, "linux")?.name)
        assertEquals("eimusic.exe", selectUpdateAsset(legacyAssets, PlatformType.Native, "windows")?.name)
    }

    @Test
    fun nativeLinuxAndWindowsPreferPlatformSpecificAssets() {
        val mixedAssets = listOf(
            GitHubAsset("eimusic.exe", "https://example.test/eimusic.exe"),
            GitHubAsset("eimusic-windows-x64.exe", "https://example.test/eimusic-windows-x64.exe"),
            GitHubAsset("eimusic.kexe", "https://example.test/eimusic.kexe"),
            GitHubAsset("eimusic-linux-x64.kexe", "https://example.test/eimusic-linux-x64.kexe"),
        )
        assertEquals("eimusic-linux-x64.kexe", selectUpdateAsset(mixedAssets, PlatformType.Native, "linux")?.name)
        assertEquals("eimusic-windows-x64.exe", selectUpdateAsset(mixedAssets, PlatformType.Native, "windows")?.name)
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

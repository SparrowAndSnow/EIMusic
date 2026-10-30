package com.eimsound.eimusic.network

import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.version.GitHubAsset

/**
 * 从 GitHub Release 资产中选择当前平台应下载的文件。
 *
 * 没有匹配资产时返回 null，调用方会回退到 Release 页面。
 */
internal fun selectUpdateAsset(
    assets: List<GitHubAsset>,
    platform: PlatformType,
    os: String,
): GitHubAsset? = when (platform) {
    // Desktop 不再单独发行，直接打开 Release 页面。
    PlatformType.DESKTOP -> null
    PlatformType.ANDROID -> assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
    PlatformType.IOS -> assets.firstOrNull { it.name.endsWith(".ipa", ignoreCase = true) }
    PlatformType.WASM -> null
    PlatformType.Native -> when {
        os.startsWith("linux", ignoreCase = true) ->
            assets.firstOrNull { it.name.endsWith(".kexe", ignoreCase = true) }
        os.startsWith("windows", ignoreCase = true) ->
            assets.firstOrNull { it.name.endsWith(".exe", ignoreCase = true) }
        // macOS terminal 暂未加入 Release；不要误选 APK 或 jar。
        os.startsWith("mac", ignoreCase = true) || os.startsWith("osx", ignoreCase = true) -> null
        else -> null
    }
}

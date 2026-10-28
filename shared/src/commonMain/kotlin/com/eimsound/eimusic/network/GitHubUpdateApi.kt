package com.eimsound.eimusic.network

import com.eimsound.eimusic.PlatformType
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.version.GitHubAsset
import com.eimsound.eimusic.version.GitHubRelease
import com.eimsound.eimusic.version.UpdateInfo
import com.eimsound.eimusic.version.UpdateResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/**
 * 直接读取 GitHub Releases，避免服务端成为版本检查的单点。
 * 仓库必须是公开的，否则匿名 API 会返回 404。
 */
class GitHubUpdateApi(
    private val httpClient: HttpClientProvider,
    private val repository: String = "SparrowAndSnow/EIMusic",
) : UpdateApi {
    override suspend fun checkUpdate(
        currentVersion: String,
        platformType: PlatformType,
        os: String,
    ): NetworkResult<UpdateResponse> = try {
        val response = httpClient.anonymousClient.get("https://api.github.com/repos/" + repository + "/releases/latest") {
            headers.append("Accept", "application/vnd.github+json")
            headers.append("User-Agent", "EIMusic-UpdateCheck")
        }
        when {
            response.status == HttpStatusCode.NotFound -> {
                // 仓库还没有任何 Release 时视为无更新。
                NetworkResult.Success(UpdateResponse(false, null, currentVersion))
            }
            !response.status.isSuccess() -> {
                NetworkResult.Error(response.status.value, "GitHub release: " + response.status.description)
            }
            else -> {
                val release = response.body<GitHubRelease>()
                val version = release.tagName.removePrefix("v").removePrefix("V")
                if (compareVersions(currentVersion, version) >= 0) {
                    NetworkResult.Success(UpdateResponse(false, null, version))
                } else {
                    val asset = selectAsset(release.assets, platformType, os)
                    NetworkResult.Success(
                        UpdateResponse(
                            hasUpdate = true,
                            update = UpdateInfo(
                                version = version,
                                versionCode = 0,
                                title = release.name ?: ("EIMusic " + version),
                                description = release.body ?: "发现新版本",
                                releaseDate = release.publishedAt?.take(10) ?: "",
                                downloadUrl = asset?.browserDownloadUrl ?: release.htmlUrl,
                                isCritical = false,
                                releaseNotes = release.body,
                            ),
                            latestVersion = version,
                        ),
                    )
                }
            }
        }
    } catch (e: Exception) {
        NetworkResult.Exception(e.message)
    }

    private fun selectAsset(assets: List<GitHubAsset>, platform: PlatformType, os: String): GitHubAsset? {
        val keyword = when (platform) {
            PlatformType.ANDROID -> ".apk"
            PlatformType.IOS -> ".ipa"
            PlatformType.DESKTOP -> when {
                os.startsWith("linux") -> "linux"
                os.startsWith("windows") -> "windows"
                os.startsWith("osx") || os.startsWith("mac") -> "macos"
                else -> ""
            }
            PlatformType.WASM, PlatformType.Native -> ""
            else -> ""
        }
        return assets.firstOrNull { it.name.contains(keyword, ignoreCase = true) }
            ?: if (platform == PlatformType.ANDROID) {
                assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            } else {
                null
            }
    }

    private fun compareVersions(a: String, b: String): Int {
        val left = parse(a)
        val right = parse(b)
        val size = maxOf(left.size, right.size)
        for (i in 0 until size) {
            val diff = left.getOrElse(i) { 0 } - right.getOrElse(i) { 0 }
            if (diff != 0) return diff
        }
        return 0
    }

    private fun parse(v: String): List<Int> =
        v.removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
}

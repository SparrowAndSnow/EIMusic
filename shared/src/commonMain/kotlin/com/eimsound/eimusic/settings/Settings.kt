package com.eimsound.eimusic.settings

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    var playMode: String,
    var volume: Double,
    var isMuted: Boolean,
    var localPath: List<String>,
    var darkMode: Boolean,
    var themeFollowSystem: Boolean,
    var language: String,
    var proxyHost: String? = null,
    var proxyPort: Int = 8080,
    var proxyEnabled: Boolean = false
)
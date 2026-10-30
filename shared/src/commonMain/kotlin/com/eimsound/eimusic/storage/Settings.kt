package com.eimsound.eimusic.storage

import com.eimsound.eimusic.music.PlayMode
import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    var playMode: String,
    var volume: Float,
    var isMuted: Boolean,
    var localPath: Set<String>,
    var navDrawerExpanded: Boolean,
    var playlists: Map<String, String> = emptyMap(),
    var darkMode: Boolean,
    var themeFollowSystem: Boolean,
    var language: String,
    var proxyHost: String? = null,
    var proxyPort: Int = 8080,
    var proxyEnabled: Boolean = false,
    var enabledNotification: Boolean = true,
    var enabledNotificationSound: Boolean = true,
) : Store {
    companion object {
        val DEFAULT = Settings(
            playMode = PlayMode.Loop.toString(),
            volume = 1.0F,
            isMuted = false,
            localPath = emptySet(),
            navDrawerExpanded = true,
            darkMode = false,
            themeFollowSystem = true,
            language = "zh",
            proxyHost = null,
            proxyPort = 8080,
            proxyEnabled = false,
            enabledNotification = true,
            enabledNotificationSound = true,
        )
    }
}
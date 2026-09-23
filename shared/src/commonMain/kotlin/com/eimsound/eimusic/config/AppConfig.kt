package com.eimsound.eimusic.config

import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.storage.Store
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val server: ServerConfig = ServerConfig(),
    val sources: Map<TrackSource, List<String>> = mapOf(
        TrackSource.NETEASE to listOf("https://netease-source.com")
    )
) : Store {
    @Serializable
    data class ServerConfig(
        val http: String = "https://your-server.com",
        val ws: String = "wss://your-server.com"
    )
}

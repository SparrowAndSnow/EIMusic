package com.eimsound.eimusic.server

import com.eimsound.eimusic.config.AppConfig
import com.eimsound.eimusic.network.NeteaseApi
import com.eimsound.eimusic.network.NeteaseApiImpl
import com.eimsound.eimusic.network.FallbackHttpClientFactory
import com.eimsound.eimusic.server.handler.WebSocketHandler
import com.eimsound.eimusic.server.manager.ChannelManager
import com.eimsound.eimusic.server.manager.UserSessionManager
import com.eimsound.eimusic.server.monitor.ConnectionMonitor
import com.eimsound.eimusic.network.RequestUrlProvider
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import java.io.File
import com.eimsound.eimusic.music.TrackSource
import kotlin.time.Duration.Companion.seconds

private fun loadAppConfig(): AppConfig {
    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    val configFile = File("eimusic.json")
    if (configFile.exists()) {
        return try {
            json.decodeFromString(configFile.readText())
        } catch (_: Exception) {
            AppConfig()
        }
    }
    configFile.writeText(json.encodeToString(AppConfig()))
    return AppConfig()
}

data class ServerSettings(
    val maxChannels: Int = 1000,
    val maxUsersPerChannel: Int = 50,
    val maxConnections: Int = 10000,
    val heartbeatIntervalSeconds: Int = 30,
)

private fun Application.loadServerSettings(): ServerSettings {
    fun int(path: String, default: Int): Int =
        environment.config.propertyOrNull(path)?.getString()?.toIntOrNull() ?: default

    return ServerSettings(
        maxChannels = int("server.maxChannels", 1000),
        maxUsersPerChannel = int("server.maxUsersPerChannel", 50),
        maxConnections = int("server.maxConnections", 10000),
        heartbeatIntervalSeconds = int("server.heartbeatInterval", 30),
    )
}

fun Application.configureFrameworks() {
    val appConfig = loadAppConfig()
    val serverSettings = loadServerSettings()
    install(Koin) {
        slf4jLogger()
        modules(module {
            single<ServerSettings> { serverSettings }
            single<ChannelManager> {
                ChannelManager(
                    maxChannels = get<ServerSettings>().maxChannels,
                    maxUsersPerChannel = get<ServerSettings>().maxUsersPerChannel,
                )
            }
            single<UserSessionManager> {
                val settings = get<ServerSettings>()
                UserSessionManager(
                    maxConnections = settings.maxConnections,
                    heartbeatTimeout = settings.heartbeatIntervalSeconds.seconds * 3,
                )
            }
            single<ConnectionMonitor>(createdAtStart = true) {
                ConnectionMonitor(get(), get())
            }
            single<WebSocketHandler> {
                WebSocketHandler(get(), get())
            }
            single<RequestUrlProvider> {
                RequestUrlProvider(appConfig.sources[TrackSource.NETEASE] ?: emptyList())
            }
            single<NeteaseApi> {
                val requestUrlProvider = get<RequestUrlProvider>()
                val logger = LoggerFactory.getLogger("NeteaseApi")
                val client = FallbackHttpClientFactory.create(requestUrlProvider) { failed, next ->
                    logger.info("Switching netease domain: $failed -> $next")
                }
                NeteaseApiImpl(requestUrlProvider.url, client)
            }
        })
    }
}

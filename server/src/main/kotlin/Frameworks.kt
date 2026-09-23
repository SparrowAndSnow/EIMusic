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

fun Application.configureFrameworks() {
    val appConfig = loadAppConfig()
    install(Koin) {
        slf4jLogger()
        modules(module {
            single<ChannelManager> {
                ChannelManager()
            }
            single<UserSessionManager> {
                UserSessionManager()
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

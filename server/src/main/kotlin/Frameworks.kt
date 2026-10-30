package com.eimsound.eimusic.server

import com.eimsound.eimusic.network.NeteaseApi
import com.eimsound.eimusic.network.NeteaseApiImpl
import com.eimsound.eimusic.network.FallbackHttpClientFactory
import com.eimsound.eimusic.server.admin.AdminAccountService
import com.eimsound.eimusic.server.db.AuditRepository
import com.eimsound.eimusic.server.db.ServerDatabase
import com.eimsound.eimusic.server.db.SourceRepository
import com.eimsound.eimusic.server.db.UserRepository
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
import com.eimsound.eimusic.music.TrackSource
import kotlin.time.Duration.Companion.seconds

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
    val serverSettings = loadServerSettings()
    val database = ServerDatabase.connect()
    val sourceRepository = SourceRepository(database)
    sourceRepository.ensureDefaults()
    val sources = sourceRepository.load()
    val userRepository = UserRepository(database)
    val adminAccounts = AdminAccountService(userRepository)
    install(Koin) {
        slf4jLogger()
        modules(module {
            single { database }
            single { sourceRepository }
            single { userRepository }
            single { adminAccounts }
            single { AuditRepository(get()) }
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
                RequestUrlProvider(
                    sources[TrackSource.NETEASE]
                        ?: SourceRepository.DEFAULT_SOURCES.getValue(TrackSource.NETEASE)
                )
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

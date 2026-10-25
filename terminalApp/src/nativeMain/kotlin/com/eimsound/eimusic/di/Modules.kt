package com.eimsound.eimusic.di

import com.eimsound.eimusic.audio.NativeAudioEngine
import com.eimsound.eimusic.audio.PlayerController
import com.eimsound.eimusic.auth.AuthStore
import com.eimsound.eimusic.config.AppConfig
import com.eimsound.eimusic.db.DbLibraryRepository
import com.eimsound.eimusic.db.DbPlaylistRepository
import com.eimsound.eimusic.db.DbSessionRepository
import com.eimsound.eimusic.db.createEIMusicDatabase
import com.eimsound.eimusic.factory.ShareLinkResolverFactory
import com.eimsound.eimusic.factory.TrackRepositoryFactory
import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.network.AuthApi
import com.eimsound.eimusic.network.AuthApiImpl
import com.eimsound.eimusic.network.HttpClientProvider
import com.eimsound.eimusic.network.NeteaseApi
import com.eimsound.eimusic.network.NeteaseApiImpl
import com.eimsound.eimusic.network.FallbackHttpClientFactory
import com.eimsound.eimusic.network.RequestUrlProvider
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.repository.NeteaseTrackRepository
import com.eimsound.eimusic.repository.TrackRepository
import com.eimsound.eimusic.resolver.track.LocalUriResolver
import com.eimsound.eimusic.resolver.track.NeteaseUriResolver
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.NativeStorage
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.network.TogetherWebSocketClient
import com.eimsound.eimusic.resolver.share.NeteaseShareLinkResolver
import com.eimsound.eimusic.together.TogetherStore
import com.eimsound.eimusic.together.TogetherSyncManager
import com.eimsound.eimusic.util.TuiLogger
import com.eimsound.eimusic.viewmodel.AuthViewModel
import com.eimsound.eimusic.viewmodel.ChannelViewModel
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.eimsound.eimusic.viewmodel.PlaylistViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.SearchViewModel
import org.koin.core.context.startKoin
import org.koin.core.KoinApplication
import org.koin.dsl.module

fun systemModule() = module {
    single { NativeAudioEngine() }
    single<IMediaPlayerController> { PlayerController(get(), get<HttpClientProvider>().client) }
    single<IStorage> { NativeStorage() }
    single(createdAtStart = true) { createEIMusicDatabase() }
    single { DbPlaylistRepository(get()) }
    single { DbLibraryRepository(get()) }
    single { DbSessionRepository(get()) }
    single<AppConfig> { get<IStorage>().get(AppConfig()) }
    single { HttpClientProvider("${get<AppConfig>().server.http}/auth/refresh", get()) }
    single<AuthApi> { AuthApiImpl(get<AppConfig>().server.http, get()) }
    single { AuthStore(lazy { get() }, get()) }
}

fun networkModule() = module {
    single { RequestUrlProvider(get<AppConfig>().sources[TrackSource.NETEASE] ?: emptyList()) }
    single<NeteaseApi> {
        val requestUrlProvider = get<RequestUrlProvider>()
        NeteaseApiImpl(requestUrlProvider.url, FallbackHttpClientFactory.create(requestUrlProvider) { failed, next ->
            TuiLogger.log("Switching netease domain: $failed -> $next")
        })
    }
}

val repositoryModule = module {
    single<TrackRepository> { NeteaseTrackRepository(get()) }
    single { TrackRepositoryFactory }
    single { TrackUriResolverFactory }
    single { ShareLinkResolverFactory }
}

fun viewModelModule() = module {
    single { PlayerViewModel(get(), get(), get(), get(), get()) }
    single { SearchViewModel(get(), get(), get()) }
    single { AuthViewModel(get()) }
    single { TogetherWebSocketClient(get<AppConfig>().server.ws, get(), get()) }
    single { TogetherSyncManager(get(), get()) }
    single { TogetherStore(get(), get(), get(), TuiLogger::log) }
    single { PlaylistViewModel(get(), get(), get(), get()) }
    single { LocalViewModel(get()) }
    single { ChannelViewModel(get()) }
}

fun initKoin(): KoinApplication = startKoin {
    modules(systemModule(), networkModule(), repositoryModule, viewModelModule())
}.also { koinApp ->
    val koin = koinApp.koin
    TrackRepositoryFactory.register(TrackSource.NETEASE, koin.get<TrackRepository>())
    TrackUriResolverFactory.register(TrackSource.NETEASE, NeteaseUriResolver(koin.get()))
    TrackUriResolverFactory.register(TrackSource.LOCAL, LocalUriResolver())

    ShareLinkResolverFactory.register(TrackSource.NETEASE, NeteaseShareLinkResolver())
}

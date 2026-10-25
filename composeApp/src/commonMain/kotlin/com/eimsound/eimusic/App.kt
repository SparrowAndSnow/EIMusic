package com.eimsound.eimusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import com.eimsound.eimusic.auth.AuthStore
import com.eimsound.eimusic.data.Storage
import com.eimsound.eimusic.data.createEIMusicDatabase
import com.eimsound.eimusic.db.DbLibraryRepository
import com.eimsound.eimusic.db.DbPlaylistRepository
import com.eimsound.eimusic.db.DbSessionRepository
import com.eimsound.eimusic.lang.Locale
import com.eimsound.eimusic.layout.BottomBar
import com.eimsound.eimusic.layout.DefaultLayout
import com.eimsound.eimusic.layout.SideBar
import com.eimsound.eimusic.layout.SidebarComponent
import com.eimsound.eimusic.layout.TopBar
import com.eimsound.eimusic.media.MediaPlayerController
import com.eimsound.eimusic.media.NotificationService
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.config.AppConfig
import com.eimsound.eimusic.network.AuthApi
import com.eimsound.eimusic.network.AuthApiImpl
import com.eimsound.eimusic.network.RequestUrlProvider
import com.eimsound.eimusic.network.HttpClientProvider
import com.eimsound.eimusic.network.NeteaseApi
import com.eimsound.eimusic.network.NeteaseApiImpl
import com.eimsound.eimusic.network.FallbackHttpClientFactory
import com.eimsound.eimusic.network.UpdateApi
import com.eimsound.eimusic.network.UpdateApiImpl
import com.eimsound.eimusic.repository.TrackRepository
import com.eimsound.eimusic.route.LocalRoute
import com.eimsound.eimusic.route.MyRoute
import com.eimsound.eimusic.route.Route
import com.eimsound.eimusic.route.SettingRoute
import com.eimsound.eimusic.route.TogetherRoute
import com.eimsound.eimusic.route.WelcomeRoute
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.theme.EIMusicTheme
import com.eimsound.eimusic.theme.Theme
import com.eimsound.eimusic.network.TogetherWebSocketClient
import com.eimsound.eimusic.repository.NeteaseTrackRepository
import com.eimsound.eimusic.resolver.share.NeteaseShareLinkResolver
import com.eimsound.eimusic.resolver.track.LocalUriResolver
import com.eimsound.eimusic.resolver.track.NeteaseUriResolver
import com.eimsound.eimusic.resolver.track.TrackUriResolver
import com.eimsound.eimusic.route.SearchResultRoute
import com.eimsound.eimusic.util.ProxyUtils
import com.eimsound.eimusic.factory.ShareLinkResolverFactory
import com.eimsound.eimusic.factory.TrackRepositoryFactory
import com.eimsound.eimusic.util.Toast
import com.eimsound.eimusic.util.ToastMessage
import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.playback.PlaybackStore
import com.eimsound.eimusic.playlist.PlaylistStore
import com.eimsound.eimusic.resolver.share.ShareLinkResolver
import com.eimsound.eimusic.together.TogetherStore
import com.eimsound.eimusic.together.TogetherSyncManager
import com.eimsound.eimusic.util.UpdateService
import com.eimsound.eimusic.viewmodel.AuthViewModel
import com.eimsound.eimusic.viewmodel.ChannelViewModel
import com.eimsound.eimusic.viewmodel.ConfigViewModel
import com.eimsound.eimusic.viewmodel.DefaultLayoutViewModel
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.eimsound.eimusic.viewmodel.MyViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import com.eimsound.eimusic.viewmodel.SearchResultViewModel
import com.eimsound.eimusic.viewmodel.SettingViewModel
import com.eimsound.eimusic.viewmodel.TrackListViewModel
import com.eimsound.eimusic.viewmodel.UpdateViewModel
import com.eimsound.eimusic.viewmodel.WelcomeViewModel
import com.eimsound.eimusic.views.ChannelView
import com.eimsound.eimusic.views.FullScreenPlayer
import com.eimsound.eimusic.views.LocalView
import com.eimsound.eimusic.views.MyView
import com.eimsound.eimusic.views.SearchResultView
import com.eimsound.eimusic.views.SettingView
import com.eimsound.eimusic.views.WelcomeView
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

@Composable
fun App(
    windowFrame: @Composable (
        content: @Composable (windowInset: WindowInsets, contentInset: WindowInsets) -> Unit
    ) -> Unit = { it(WindowInsets(), WindowInsets()) },
) {
    AppEnvironment {
        EIMusicTheme {
            val defaultLayoutViewModel = koinViewModel<DefaultLayoutViewModel>()
            val authStore = koinInject<AuthStore>()
            val sideBarState by defaultLayoutViewModel.sideBarState.collectAsState()
            val navigationDrawerState by defaultLayoutViewModel.navigationDrawerState.collectAsState()
            val showFullScreenPlayer by defaultLayoutViewModel.fullScreenPlayerState.collectAsState()
            val backStack: MutableList<Route> = rememberSerializable(serializer = SnapshotStateListSerializer()) {
                mutableStateListOf(WelcomeRoute)
            }
//            authStore.onUnauthenticated {
//                Toast.show(
//                    ToastMessage("未登录", "点击登录") {
//                        backStack.add(SettingRoute(true))
//                    }
//                )
//            }
            windowFrame { windowInset, contentInset ->
                // 主界面
                DefaultLayout(
                    modifier = Modifier.statusBarsPadding()
                        .windowInsetsPadding(windowInset),
                    topBar = { TopBar({ backStack.add(SearchResultRoute(it)) }) },
                    bottomBar = { BottomBar() },
                    isNavigationDrawerExpanded = navigationDrawerState.isExpanded,
                    onToggleNavigationDrawer = { defaultLayoutViewModel.toggleDrawer() },
                    showSideBar = sideBarState.showSideBar,
                    sideBar = {
                        SideBar(sideBarState.sidebarComponent) {
                            defaultLayoutViewModel.updateSideBar(
                                false,
                                SidebarComponent.PLAYLIST
                            )
                        }
                    },
                    floatingActionButton = {
//                            FloatingActionButton()
                    },
                    backStack = backStack
                ) {
                    entry<WelcomeRoute> {
                        WelcomeView()
                    }
                    entry<SearchResultRoute> {
                        SearchResultView(it.query)
                    }
                    entry<MyRoute> {
                        MyView()
                    }
                    entry<LocalRoute> {
                        LocalView()
                    }
                    entry<TogetherRoute> {
                        ChannelView(onBack = { /* 返回处理 */ })
                    }
                    entry<SettingRoute> {
                        SettingView(it.showLogin)
                    }
                }
            }

            // 全屏播放界面
            AnimatedVisibility(
                visible = showFullScreenPlayer.isShow,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FullScreenPlayer(
                    onDismiss = { defaultLayoutViewModel.updateFullScreenPlayer(false) }
                )
            }
        }
    }
}


@Composable
fun AppEnvironment(content: @Composable () -> Unit) {
    val settingViewModel = koinViewModel<SettingViewModel>()
    val themeState by settingViewModel.themeState.collectAsState()
    val languageState by settingViewModel.languageState.collectAsState()
    val proxyState by settingViewModel.proxyState.collectAsState()
    val isSystemInDarkTheme = Theme.isSystemInDarkTheme
    val isDarkTheme = if (themeState.themeFollowSystem) {
        isSystemInDarkTheme
    } else {
        themeState.darkMode
    }

    ProxyUtils.configureSystemProxy(proxyState.proxyEnabled, proxyState.proxyHost, proxyState.proxyPort)

    CompositionLocalProvider(
        Locale provides languageState.language,
        Theme provides isDarkTheme
    ) {
        content()
    }
}

val systemModule = module {
    single<IStorage>(createdAtStart = true) { Storage() }

    single(createdAtStart = true) { createEIMusicDatabase() }
    single { DbPlaylistRepository(get()) }
    single { DbLibraryRepository(get()) }
    single { DbSessionRepository(get()) }

    single<AppConfig> { get<IStorage>().get(AppConfig()) }

    single<IMediaPlayerController>(createdAtStart = true) { MediaPlayerController(get()) }

    single<AuthStore> { AuthStore(lazy { get() }, get()) }

    single<TogetherSyncManager> { TogetherSyncManager(get(), get()) }
    single { TogetherStore(get(), get(), get()) }

    single<NotificationService> {
        NotificationService(get<SettingViewModel>().notificationState)
    }
}

fun networkModule() = module {
    single<RequestUrlProvider> {
        RequestUrlProvider(get<AppConfig>().sources[TrackSource.NETEASE] ?: emptyList())
    }
    single<AuthApi> { AuthApiImpl(get<AppConfig>().server.http, get()) }

    single<HttpClientProvider> { HttpClientProvider("${get<AppConfig>().server.http}/auth/refresh", get()) }

    single<TogetherWebSocketClient> { TogetherWebSocketClient(get<AppConfig>().server.ws, get(), get()) }

    single<UpdateApi> { UpdateApiImpl(get<AppConfig>().server.http, get()) }

    single<UpdateService> { UpdateService(get()) }
}

val externalApiModule = module {
    single<NeteaseApi> {
        val requestUrlProvider = get<RequestUrlProvider>()
        NeteaseApiImpl(requestUrlProvider.url, FallbackHttpClientFactory.create(requestUrlProvider))
    }
}

val repositoryModule = module {
    single<TrackUriResolver>(named(TrackSource.NETEASE)) { NeteaseUriResolver(get(named(TrackSource.NETEASE))) }
    single<TrackUriResolver>(named(TrackSource.LOCAL)) { LocalUriResolver() }

    single<ShareLinkResolver>(named(TrackSource.NETEASE)) { NeteaseShareLinkResolver() }

    single<TrackRepository>(named(TrackSource.NETEASE)) { NeteaseTrackRepository(get()) }

    // Keep factory references in Koin for backward compatibility with existing ViewModels.
    // The factories themselves are now object singletons in shared/ — they don't need Koin.
    single<TrackUriResolverFactory> { TrackUriResolverFactory }
    single<ShareLinkResolverFactory> { ShareLinkResolverFactory }
    single<TrackRepositoryFactory> { TrackRepositoryFactory }
}

val viewModelModule = module {
    single(createdAtStart = true) { SettingViewModel(get()) }
    single(createdAtStart = true) { DefaultLayoutViewModel(get()) }
    single(createdAtStart = true) { PlayingListViewModel(get(), get()) }
    single { PlaybackStore(get(), get(), get(), get(), get()) }
    single(createdAtStart = true) { PlayerViewModel(get(), get(), get()) }
    single { ConfigViewModel(get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { ChannelViewModel(get(), get()) }
    viewModel { SearchResultViewModel(get(), get()) }
    single { PlaylistStore(get(), get(), get(), get()) }
    viewModel { MyViewModel(get()) }
    viewModel { TrackListViewModel(get(), get()) }
    viewModel { WelcomeViewModel(get()) }
    viewModel { LocalViewModel(get()) }
    viewModel { UpdateViewModel(get(), AppVersion.VERSION_NAME, getPlatform().type) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(systemModule, viewModelModule, networkModule(), externalApiModule, repositoryModule)
    }.also { koinApp ->
        val koin = koinApp.koin
        // Register actual resolver / repository instances in the shared factories.
        // This moves DI wiring out of the factory classes so they can be used
        // by terminalApp (Kotlin/Native) without Koin.
        TrackRepositoryFactory.register(TrackSource.NETEASE, koin.get(named(TrackSource.NETEASE)))
        TrackUriResolverFactory.register(TrackSource.NETEASE, koin.get(named(TrackSource.NETEASE)))
        TrackUriResolverFactory.register(TrackSource.LOCAL, koin.get(named(TrackSource.LOCAL)))
        ShareLinkResolverFactory.register(TrackSource.NETEASE, koin.get(named(TrackSource.NETEASE)))
    }
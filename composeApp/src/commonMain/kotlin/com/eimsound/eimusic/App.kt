package com.eimsound.eimusic

import TrackListViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import com.eimsound.eimusic.data.Storage
import com.eimsound.eimusic.lang.Locale
import com.eimsound.eimusic.layout.BottomBar
import com.eimsound.eimusic.layout.DefaultLayout
import com.eimsound.eimusic.layout.FloatingActionButton
import com.eimsound.eimusic.layout.SideBar
import com.eimsound.eimusic.media.MediaPlayerController
import com.eimsound.eimusic.repository.SpotifyTrackRepository
import com.eimsound.eimusic.repository.TrackRepository
import com.eimsound.eimusic.route.LocalRoute
import com.eimsound.eimusic.route.MyRoute
import com.eimsound.eimusic.route.Route
import com.eimsound.eimusic.route.SettingRoute
import com.eimsound.eimusic.route.WelcomeRoute
import com.eimsound.eimusic.theme.EIMusicTheme
import com.eimsound.eimusic.theme.Theme
import com.eimsound.eimusic.util.ProxyUtils
import com.eimsound.eimusic.viewmodel.DefaultLayoutViewModel
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import com.eimsound.eimusic.viewmodel.SettingViewModel
import com.eimsound.eimusic.viewmodel.WelcomeViewModel
import com.eimsound.eimusic.views.FullScreenPlayer
import com.eimsound.eimusic.views.LocalView
import com.eimsound.eimusic.views.MyView
import com.eimsound.eimusic.views.SettingView
import com.eimsound.eimusic.views.WelcomeView
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@Composable
fun App(
    windowFrame: @Composable (
        content: @Composable (windowInset: WindowInsets, contentInset: WindowInsets) -> Unit
    ) -> Unit = { it(WindowInsets(), WindowInsets()) },
) {
    KoinApplication(application = {
        modules(systemModule, viewModelModule)
    }) {
        AppEnvironment {
            EIMusicTheme {
                val defaultLayoutViewModel = koinViewModel<DefaultLayoutViewModel>()
                val sideBarState by defaultLayoutViewModel.sideBarState.collectAsState()
                val showFullScreenPlayer by defaultLayoutViewModel.fullScreenPlayerState.collectAsState()
                val backStack: MutableList<Route> = rememberSerializable(serializer = SnapshotStateListSerializer()) {
                    mutableStateListOf(WelcomeRoute)
                }
                windowFrame { windowInset, contentInset ->
                    // 主界面
                    DefaultLayout(
                        modifier = Modifier
                            .windowInsetsPadding(windowInset),
                        topBar = { },
                        bottomBar = { BottomBar() },
                        showSideBar = sideBarState.showSideBar,
                        sideBar = { SideBar(sideBarState.sidebarComponent) },
                        floatingActionButton = {
                            FloatingActionButton()
                        },
                        backStack = backStack
                    ) {
                        entry<WelcomeRoute> {
                            WelcomeView()
                        }
                        entry<MyRoute> {
                            MyView()
                        }
                        entry<LocalRoute> {
                            LocalView()
                        }
                        entry<SettingRoute> {
                            SettingView()
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
    single<Storage> { Storage() }
    single<MediaPlayerController> { MediaPlayerController() }
}

val viewModelModule = module {
    single(createdAtStart = true) { SettingViewModel(get()) }
    single(createdAtStart = true) { DefaultLayoutViewModel() }
    single(createdAtStart = true) { PlayingListViewModel() }
    single(createdAtStart = true) { PlayerViewModel(get(), get()) }

    single<TrackRepository> { SpotifyTrackRepository() }
    viewModel { TrackListViewModel() }
    viewModel { WelcomeViewModel(get()) }
    viewModel { LocalViewModel(get()) }
}
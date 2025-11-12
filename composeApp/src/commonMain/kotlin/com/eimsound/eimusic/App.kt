package com.eimsound.eimusic

import TrackListViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eimsound.eimusic.data.Storage
import com.eimsound.eimusic.lang.Locale
import com.eimsound.eimusic.layout.BottomBar
import com.eimsound.eimusic.layout.DefaultLayout
import com.eimsound.eimusic.layout.FloatingActionButton
import com.eimsound.eimusic.layout.SideBar
import com.eimsound.eimusic.media.MediaPlayerController
import com.eimsound.eimusic.repository.SpotifyTrackRepository
import com.eimsound.eimusic.repository.TrackRepository
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
import com.eimsound.eimusic.views.LocalRoute
import com.eimsound.eimusic.views.LocalView
import com.eimsound.eimusic.views.MyRoute
import com.eimsound.eimusic.views.MyView
import com.eimsound.eimusic.views.SettingRoute
import com.eimsound.eimusic.views.SettingView
import com.eimsound.eimusic.views.WelcomeRoute
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
                val navController = rememberNavController()

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
                        navController = navController
                    ) {
                        composable<WelcomeRoute> { WelcomeView() }
                        composable<MyRoute> { MyView() }
                        composable<LocalRoute> { LocalView() }
                        composable<SettingRoute> { SettingView() }
                    }
                }
                // 全屏播放界面
                if (showFullScreenPlayer.isShow) {
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
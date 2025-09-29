package com.eimsound.eimusic

import androidx.compose.runtime.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eimsound.eimusic.data.Storage
import com.eimsound.eimusic.layout.BottomBar
import com.eimsound.eimusic.layout.DefaultLayout
import com.eimsound.eimusic.layout.SideBar
import com.eimsound.eimusic.layout.TopBar
import com.eimsound.eimusic.media.MediaPlayerController
import com.eimsound.eimusic.theme.EIMusicTheme
import com.eimsound.eimusic.viewmodel.DefaultLayoutViewModel
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import com.eimsound.eimusic.views.LocalRoute
import com.eimsound.eimusic.views.LocalView
import com.eimsound.eimusic.views.ProfileRoute
import com.eimsound.eimusic.views.ProfileView
import com.eimsound.eimusic.views.WelcomeRoute
import com.eimsound.eimusic.views.WelcomeView
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@Composable
fun App() {
    KoinApplication(application = {
        modules(systemModule, viewModelModule)
    }) {
        EIMusicTheme {
            val defaultLayoutViewModel = koinViewModel<DefaultLayoutViewModel>()
            val sideBarState by defaultLayoutViewModel.sideBarState.collectAsState()
            val navController = rememberNavController()
            DefaultLayout(
                topBar = { TopBar() },
                bottomBar = { BottomBar() },
                showSideBar = sideBarState.showSideBar,
                sideBar = { SideBar(sideBarState.sidebarComponent) },
                navController = navController
            ) {
                composable<WelcomeRoute> { WelcomeView() }
                composable<ProfileRoute> { ProfileView() }
                composable<LocalRoute> { LocalView() }
            }
        }
    }

}

val systemModule = module {
    single<Storage> { Storage() }
    single<MediaPlayerController> { MediaPlayerController() }
}

val viewModelModule = module {
    viewModel { DefaultLayoutViewModel() }
    viewModel { PlayerViewModel(get(),get()) }
    viewModel { PlayingListViewModel() }
    viewModel { LocalViewModel(get()) }
}

package com.eimsound.eimusic

import androidx.compose.runtime.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.eimsound.eimusic.layout.BottomBar
import com.eimsound.eimusic.layout.DefaultLayout
import com.eimsound.eimusic.layout.SideBar
import com.eimsound.eimusic.layout.TopBar
import com.eimsound.eimusic.media.MediaPlayerController
import com.eimsound.eimusic.theme.EIMusicTheme
import com.eimsound.eimusic.viewmodel.DefaultLayoutViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import com.eimsound.eimusic.views.ProfileRoute
import com.eimsound.eimusic.views.ProfileView
import com.eimsound.eimusic.views.WelcomeRoute
import com.eimsound.eimusic.views.WelcomeView
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@Composable
@Preview
fun App() {
    KoinApplication(application = {
        modules(systemModule, viewModelModule)
    }) {
        EIMusicTheme {
            val defaultLayoutViewModel = koinViewModel<DefaultLayoutViewModel>()
            val navController = rememberNavController()
            DefaultLayout(
                topBar = { TopBar() },
                bottomBar = { BottomBar() },
                showSideBar = defaultLayoutViewModel.showSideBar,
                sideBar = { SideBar() },
                navController = navController
            ) {
                composable<WelcomeRoute> { WelcomeView() }
                composable<ProfileRoute> { ProfileView() }
            }
        }
    }

}

val systemModule = module {
    single<MediaPlayerController> { MediaPlayerController() }
}

val viewModelModule = module {
    viewModel { DefaultLayoutViewModel() }
    viewModel { PlayerViewModel(get()) }
    viewModel { PlayingListViewModel() }
}

package com.eimsound.eimusic

import androidx.compose.runtime.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.eimsound.eimusic.layout.BottomBar
import com.eimsound.eimusic.layout.DefaultLayout
import com.eimsound.eimusic.layout.TopBar
import com.eimsound.eimusic.media.rememberMediaPlayerController
import com.eimsound.eimusic.theme.EIMusicTheme
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import com.eimsound.eimusic.views.ProfileRoute
import com.eimsound.eimusic.views.ProfileView
import com.eimsound.eimusic.views.WelcomeRoute
import com.eimsound.eimusic.views.WelcomeView
import org.koin.compose.KoinApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@Composable
@Preview
fun App() {
    KoinApplication(application = {
        // your preview config here
        modules(viewModelModule)
    }) {
        EIMusicTheme {
            val navController = rememberNavController()
            val rememberMediaPlayerController = rememberMediaPlayerController()
            DefaultLayout(
                topBar = { TopBar() },
                bottomBar = { BottomBar(rememberMediaPlayerController) },
                navController = navController
            ) {
                composable<WelcomeRoute> { WelcomeView() }
                composable<ProfileRoute> { ProfileView() }
            }
        }
    }

}

val viewModelModule = module {
    viewModel {
        PlayerViewModel()
    }
    viewModel {
        PlayingListViewModel()
    }
}

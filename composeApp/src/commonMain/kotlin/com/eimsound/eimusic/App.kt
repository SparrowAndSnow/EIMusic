package com.eimsound.eimusic

import androidx.compose.runtime.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.eimsound.eimusic.layout.BottomBar
import com.eimsound.eimusic.layout.DefaultLayout
import com.eimsound.eimusic.layout.TopBar
import com.eimsound.eimusic.theme.EIMusicTheme
import com.eimsound.eimusic.views.ProfileRoute
import com.eimsound.eimusic.views.ProfileView
import com.eimsound.eimusic.views.WelcomeRoute
import com.eimsound.eimusic.views.WelcomeView

@Composable
@Preview
fun App() {
    EIMusicTheme {
        val navController = rememberNavController()
        DefaultLayout(
            topBar = { TopBar() },
            bottomBar = { BottomBar() },
            navController = navController
        ) {
            composable<WelcomeRoute> { WelcomeView() }
            composable<ProfileRoute> { ProfileView() }
        }
    }
}

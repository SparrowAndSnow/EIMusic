package com.eimsound.eimusic.layout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.eimsound.eimusic.components.Navigation
import com.eimsound.eimusic.components.navigationLayoutType
import com.eimsound.eimusic.views.WelcomeRoute

@Composable
fun DefaultLayout(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    navController: NavHostController,
    defaultRoute: Any = WelcomeRoute,
    builder: NavGraphBuilder.() -> Unit
) {
    Navigation(navController, navigationLayoutType()) {
        ContentLayout(topBar, bottomBar, floatingActionButton) {
            NavHost(
                navController = navController,
                startDestination = defaultRoute
            ) {
                builder()
            }
        }
    }
}

@Composable
private fun ContentLayout(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            Row(Modifier.systemBarsPadding().fillMaxWidth()) {
                topBar()
            }
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth()) {
                bottomBar()
            }
        },
        floatingActionButton = {
            floatingActionButton()
        }
    ) { innerPadding ->
        Row(Modifier.padding(innerPadding)) {
            content()
        }
    }
}

package com.eimsound.eimusic.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.eimsound.eimusic.components.Navigation
import com.eimsound.eimusic.components.navigationLayoutType
import com.eimsound.eimusic.views.WelcomeRoute

@Composable
fun DefaultLayout(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    showSideBar: Boolean = false,
    sideBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    navController: NavHostController,
    defaultRoute: Any = WelcomeRoute,
    builder: NavGraphBuilder.() -> Unit
) {
    Navigation(modifier = modifier, navController = navController, navigationLayoutType()) {
        ContentLayout(topBar, bottomBar, showSideBar, sideBar, floatingActionButton) {
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
    showSideBar: Boolean = false,
    sideBar: @Composable () -> Unit,
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
        Row(
            Modifier.padding(innerPadding).fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(Modifier.widthIn(min = 500.dp).weight(1f).animateContentSize()) {
                content()
            }

            AnimatedVisibility(
                visible = showSideBar,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Surface(Modifier.width(300.dp)) {
                    sideBar()
                }
            }
        }
    }
}

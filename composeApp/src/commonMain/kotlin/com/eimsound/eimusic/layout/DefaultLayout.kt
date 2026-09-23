package com.eimsound.eimusic.layout

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.eimsound.eimusic.components.Navigation
import com.eimsound.eimusic.components.ToastView
import com.eimsound.eimusic.components.navigationLayoutType
import com.eimsound.eimusic.route.Route
import com.eimsound.eimusic.route.routes
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.theme.LocalRadii

@Composable
fun DefaultLayout(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    isNavigationDrawerExpanded: Boolean = true,
    onToggleNavigationDrawer: () -> Unit = {},
    showSideBar: Boolean = false,
    sideBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    backStack: MutableList<Route>,
    builder: EntryProviderScope<Route>.() -> Unit
) {
    Navigation(
        modifier = modifier.fillMaxSize(),
        backStack = backStack,
        navigationLayoutType = navigationLayoutType(),
        isDrawerExpanded = isNavigationDrawerExpanded,
        onToggleDrawer = onToggleNavigationDrawer
    ) {
        ContentLayout(topBar, bottomBar, showSideBar, sideBar, floatingActionButton) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                transitionSpec = {
                    val keys = routes.map { it.toString() }
                    val startIndex = keys.indexOf(initialState.key)
                    val targetIndex = keys.indexOf(targetState.key)
                    val isPush = targetIndex > startIndex
                    if (isPush) {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn() togetherWith
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut()
                    } else {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) + fadeIn() togetherWith
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) + fadeOut()
                    }
                },
                entryProvider = entryProvider {
                    builder()
                }
            )
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
    val palette = LocalPalette.current
    val radii = LocalRadii.current

    Scaffold(
        snackbarHost = { ToastView() },
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                topBar()
            }
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                bottomBar()
            }
        },
        floatingActionButton = {
            floatingActionButton()
        },
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f)
    ) { innerPadding ->
        if (LocalWindowInfo.current.containerDpSize.width >= 600.dp) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DefaultContainer(
                    modifier = Modifier.weight(1f)
                        .fillMaxHeight()
                        .sizeIn(minWidth = 0.dp),
                ) {
                    content()
                }
                AnimatedVisibility(
                    visible = showSideBar,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Surface(
                        modifier = Modifier
                            .sizeIn(minWidth = 280.dp, maxWidth = 320.dp)
                            .fillMaxHeight(),
                        shape = radii.large,
                        color = palette.surfaceGlass,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    ) {
                        sideBar()
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                DefaultContainer(
                    modifier = Modifier
                        .fillMaxHeight()
                        .sizeIn(minWidth = 0.dp),
                ) {
                    content()
                }

                AnimatedVisibility(
                    visible = showSideBar,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        shape = radii.large,
                        color = palette.surfaceRaised,
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                    ) {
                        sideBar()
                    }
                }
            }
        }

    }
}

@Composable
private fun DefaultContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val palette = LocalPalette.current
    val radii = LocalRadii.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.06f))
            .padding(8.dp)
    ) {
        content()
    }
}
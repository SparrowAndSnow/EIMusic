package com.eimsound.eimusic.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.eimsound.eimusic.route.routes

@Composable
fun Navigation(
    navController: NavController,
    navigationLayoutType: NavigationSuiteType, content: @Composable () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavigationSuiteScaffold(
        layoutType = navigationLayoutType,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        navigationSuiteItems = {
            routes.forEach { route ->
                item(
                    icon = { Icon(route.icon, contentDescription = route.name) },
                    label = { Text(route.name) },
                    selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(route.route::class)
                    } == true,
                    onClick = {
                        navController.navigate(route.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }

    ) {
        content()
    }
}

expect fun navigationLayoutType(): NavigationSuiteType

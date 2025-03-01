package com.eimsound.eimusic.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eimsound.eimusic.views.ProfileRoute
import com.eimsound.eimusic.views.ProfileView
import com.eimsound.eimusic.views.WelcomeRoute
import com.eimsound.eimusic.views.WelcomeView


data class  Route<T : Any>(val name: String, val route: T, val icon: ImageVector)

val routes = listOf(
    Route("WelcomeRoute", WelcomeRoute, Icons.Filled.Home),
    Route("ProfileRoute", ProfileRoute, Icons.Filled.Add),
)


@Composable
fun Navigation(navigationLayoutType: NavigationSuiteType) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavigationSuiteScaffold(
        layoutType = navigationLayoutType,
        navigationSuiteItems = {
            routes.forEach { topLevelRoute ->
                item(
                    icon = { Icon(topLevelRoute.icon, contentDescription = topLevelRoute.name) },
                    label = { Text(topLevelRoute.name) },
                    selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(topLevelRoute.route::class)
                    } == true,
                    onClick = {
                        navController.navigate(topLevelRoute.route) {
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
        NavHost(navController, startDestination = WelcomeRoute) {
            composable<WelcomeRoute> { WelcomeView() }
            composable<ProfileRoute> { ProfileView() }
        }
    }
}
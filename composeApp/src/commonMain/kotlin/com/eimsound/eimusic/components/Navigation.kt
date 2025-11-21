package com.eimsound.eimusic.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eimsound.eimusic.route.Route
import com.eimsound.eimusic.route.TopLevelRoute
import com.eimsound.eimusic.route.WelcomeRoute
import com.eimsound.eimusic.route.localizedRouteName
import com.eimsound.eimusic.route.routes

@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    backStack: MutableList<Route>,
    navigationLayoutType: NavigationSuiteType,
    content: @Composable () -> Unit = {}
) {
    var selected by remember { mutableStateOf<TopLevelRoute>(WelcomeRoute) }
    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = navigationLayoutType,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        navigationSuiteItems = {
            routes.forEach { route ->
                item(
                    icon = { Icon(route.icon, contentDescription = route.localizedRouteName()) },
                    label = { Text(route.localizedRouteName()) },
                    selected = route == selected,
                    onClick = {
                        selected = route
                        backStack.add(route)
                    }
                )
            }
        }

    ) {
        content()
    }
}

expect fun navigationLayoutType(): NavigationSuiteType
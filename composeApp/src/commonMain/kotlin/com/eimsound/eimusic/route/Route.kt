package com.eimsound.eimusic.route

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.eimsound.eimusic.views.LocalRoute
import com.eimsound.eimusic.views.ProfileRoute
import com.eimsound.eimusic.views.SettingRoute
import com.eimsound.eimusic.views.WelcomeRoute
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.route_local
import eimusic.composeapp.generated.resources.route_profile
import eimusic.composeapp.generated.resources.route_setting
import eimusic.composeapp.generated.resources.route_welcome
import org.jetbrains.compose.resources.StringResource


data class Route<T : Any>(val nameRes: StringResource, val route: T, val icon: ImageVector)

val routes = listOf(
    Route(Res.string.route_welcome, WelcomeRoute, Icons.Filled.Home),
    Route(Res.string.route_profile, ProfileRoute, Icons.Filled.Add),
    Route(Res.string.route_local, LocalRoute, Icons.Filled.Storage),
    Route(Res.string.route_setting, SettingRoute, Icons.Filled.Settings),
)

@Composable
fun Route<*>.localizedRouteName(): String {
    return org.jetbrains.compose.resources.stringResource(this.nameRes)
}
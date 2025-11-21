package com.eimsound.eimusic.route

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.route_local
import eimusic.composeapp.generated.resources.route_my
import eimusic.composeapp.generated.resources.route_setting
import eimusic.composeapp.generated.resources.route_welcome
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource


@Serializable
sealed interface Route

@Serializable
sealed interface TopLevelRoute : Route {
    val icon: ImageVector
    val name: StringResource
}

@Serializable
object LocalRoute : TopLevelRoute {
    override val icon: ImageVector = Icons.Filled.Storage
    override val name: StringResource = Res.string.route_local

}

@Serializable
object MyRoute : TopLevelRoute {
    override val icon: ImageVector = Icons.Filled.MusicNote
    override val name: StringResource = Res.string.route_my

}

@Serializable
object SettingRoute : TopLevelRoute {
    override val icon: ImageVector = Icons.Filled.Settings
    override val name: StringResource = Res.string.route_setting

}

@Serializable
object WelcomeRoute : TopLevelRoute {
    override val icon: ImageVector = Icons.Filled.Home
    override val name: StringResource = Res.string.route_welcome

}

val routes = listOf(WelcomeRoute, MyRoute, LocalRoute, SettingRoute)

@Composable
fun TopLevelRoute.localizedRouteName(): String {
    return org.jetbrains.compose.resources.stringResource(this.name)
}
package com.eimsound.eimusic.route

import androidx.compose.runtime.Composable
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.group_fill_24px
import eimusic.composeapp.generated.resources.home_fill_24px
import eimusic.composeapp.generated.resources.music_note_24px
import eimusic.composeapp.generated.resources.route_local
import eimusic.composeapp.generated.resources.route_my
import eimusic.composeapp.generated.resources.route_setting
import eimusic.composeapp.generated.resources.route_welcome
import eimusic.composeapp.generated.resources.route_together
import eimusic.composeapp.generated.resources.settings_fill_24px
import eimusic.composeapp.generated.resources.storage_24px
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource


@Serializable
sealed interface Route

@Serializable
sealed interface TopLevelRoute : Route {
    val icon: DrawableResource
    val name: StringResource
}

@Serializable
object LocalRoute : TopLevelRoute {
    override val icon: DrawableResource = Res.drawable.storage_24px
    override val name: StringResource = Res.string.route_local

}

@Serializable
object MyRoute : TopLevelRoute {
    override val icon: DrawableResource = Res.drawable.music_note_24px
    override val name: StringResource = Res.string.route_my

}

@Serializable
object TogetherRoute : TopLevelRoute {
    override val icon: DrawableResource = Res.drawable.group_fill_24px
    override val name: StringResource = Res.string.route_together
}

@Serializable
data class SettingRoute(val showLogin: Boolean = false) : TopLevelRoute {
    override val icon: DrawableResource
        get() = Res.drawable.settings_fill_24px
    override val name: StringResource
        get() = Res.string.route_setting

}

@Serializable
object WelcomeRoute : TopLevelRoute {
    override val icon: DrawableResource = Res.drawable.home_fill_24px
    override val name: StringResource = Res.string.route_welcome

}

@Serializable
data class SearchResultRoute(val query: String) : Route

val routes: List<TopLevelRoute> = listOf(WelcomeRoute, MyRoute, LocalRoute, TogetherRoute, SettingRoute())

@Composable
fun TopLevelRoute.localizedRouteName(): String {
    return stringResource(this.name)
}

package com.eimsound.eimusic.route

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.eimsound.eimusic.views.ProfileRoute
import com.eimsound.eimusic.views.WelcomeRoute


data class Route<T : Any>(val name: String, val route: T, val icon: ImageVector)

val routes = listOf(
    Route("WelcomeRoute", WelcomeRoute, Icons.Filled.Home),
    Route("ProfileRoute", ProfileRoute, Icons.Filled.Add),
)

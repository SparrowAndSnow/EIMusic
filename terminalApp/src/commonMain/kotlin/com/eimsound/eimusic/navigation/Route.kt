package com.eimsound.eimusic.navigation

sealed interface Route {
    data object Main : Route
    data class Search(val initialQuery: String = "") : Route
    data object Login : Route
    data object LogViewer : Route

    data object Help : Route

    // New features
    data object Playlists : Route
    data object LocalSongs : Route
    data object Together : Route
    data object Queue : Route
}

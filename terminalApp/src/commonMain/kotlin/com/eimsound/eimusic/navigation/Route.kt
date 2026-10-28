package com.eimsound.eimusic.navigation

sealed interface Route {
    /** 弹层路由：底层页面保留绘制，只有这层接收输入。 */
    val isOverlay: Boolean get() = this is Login || this is Help

    data object Main : Route
    data class Search(val initialQuery: String = "") : Route
    data object Login : Route
    data object LogViewer : Route

    data object Help : Route
    data object Settings : Route

    // New features
    data object Playlists : Route
    data object LocalSongs : Route
    data object Together : Route
    data object Queue : Route
}

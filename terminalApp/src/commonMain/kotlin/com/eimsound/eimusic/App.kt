package com.eimsound.eimusic

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.GlobalKeyActions
import com.eimsound.eimusic.keyboard.LocalGlobalActions
import com.eimsound.eimusic.keyboard.dispatchGlobalKey
import com.eimsound.eimusic.navigation.Route
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.ui.components.HeaderBar
import com.eimsound.eimusic.ui.components.TabBar
import com.eimsound.eimusic.ui.components.TabItem
import com.eimsound.eimusic.ui.screens.HelpScreen
import com.eimsound.eimusic.ui.screens.LocalScreen
import com.eimsound.eimusic.ui.screens.LogViewerScreen
import com.eimsound.eimusic.ui.screens.LoginScreen
import com.eimsound.eimusic.ui.screens.MainScreen
import com.eimsound.eimusic.ui.screens.PlaylistScreen
import com.eimsound.eimusic.ui.screens.QueueScreen
import com.eimsound.eimusic.ui.screens.SearchScreen
import com.eimsound.eimusic.ui.screens.TogetherScreen
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.ui.theme.SafeContentSize
import com.eimsound.eimusic.ui.theme.TuiColors
import com.eimsound.eimusic.util.bridgeToMutable
import com.eimsound.eimusic.util.render
import com.eimsound.eimusic.viewmodel.AuthViewModel
import com.eimsound.eimusic.viewmodel.ChannelViewModel
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.eimsound.eimusic.viewmodel.PlaylistViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.SearchViewModel
import com.jakewharton.mosaic.LocalTerminalState
import com.jakewharton.mosaic.layout.*
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.terminal.Terminal
import com.jakewharton.mosaic.ui.*
import kotlinx.coroutines.awaitCancellation

@Composable
fun App(
    storage: IStorage,
    playerViewModel: PlayerViewModel,
    searchViewModel: SearchViewModel,
    authViewModel: AuthViewModel,
    playlistViewModel: PlaylistViewModel,
    localViewModel: LocalViewModel,
    channelViewModel: ChannelViewModel,
    onQuit: () -> Unit = {},
) {
    // runMosaic 是 run-until-idle：没有常驻 effect 时首帧稳定后即返回、进程退出。
    LaunchedEffect(Unit) { awaitCancellation() }

    val localTerminal = LocalTerminalState.current
    val terminalTheme = localTerminal.theme
    var isDark by remember {
        mutableStateOf(
            when (terminalTheme) {
                Terminal.Theme.Dark -> true
                Terminal.Theme.Light -> false
                Terminal.Theme.Unknown -> storage.get(Settings::darkMode, true)
            }
        )
    }
    val palette = remember(isDark) { if (isDark) TuiColors.dark() else TuiColors.light() }

    var backStack by remember { mutableStateOf(listOf<Route>(Route.Main)) }
    val playerState by playerViewModel.state.bridgeToMutable()
    val togetherState by channelViewModel.uiState.bridgeToMutable()
    val currentRoute = backStack.last()

    // 弹窗类路由：保留底层页面绘制，弹窗叠在上面。
    val overlayRoute = currentRoute is Route.Help || currentRoute is Route.Search || currentRoute is Route.Login
    val baseRoute = if (overlayRoute) backStack.getOrNull(backStack.size - 2) else null

    val navigate: (Route) -> Unit = { route -> backStack = backStack + route }
    val goBack: () -> Unit = { if (backStack.size > 1) backStack = backStack.dropLast(1) }

    // 底部标签页：只放可切换的页面，弹窗类路由不算标签。
    val tabRoutes = listOf(
        "Home" to Route.Main,
        "Playlists" to Route.Playlists,
        "Local" to Route.LocalSongs,
        "Together" to Route.Together,
        "Queue" to Route.Queue,
    )
    val activeTabRoute = baseRoute ?: currentRoute

    val appGlobalActions = remember(playerViewModel) {
        GlobalKeyActions(
            onPlayPause = { playerViewModel.togglePlayPause() },
            onNext = { playerViewModel.next() },
            onPrevious = { playerViewModel.previous() },
            onVolumeUp = { playerViewModel.volumeUp() },
            onVolumeDown = { playerViewModel.volumeDown() },
            onSeekForward = { playerViewModel.seekRelative(5000L) },
            onSeekBackward = { playerViewModel.seekRelative(-5000L) },
            onTogglePlayMode = { playerViewModel.togglePlayMode() },
            onOpenQueue = { if (currentRoute !is Route.Queue) navigate(Route.Queue) },
            onOpenSearch = { if (currentRoute !is Route.Search) navigate(Route.Search()) },
        )
    }

    @Composable
    fun RouteContent(route: Route) {
        when (route) {
            is Route.Main -> {
                MainScreen(
                    playerState = playerState,
                    playerViewModel = playerViewModel,
                    onOpenHelp = { navigate(Route.Help) },
                    onOpenSearch = { navigate(Route.Search()) },
                    onOpenLogin = { navigate(Route.Login) },
                    onOpenLogs = { navigate(Route.LogViewer) },
                    onOpenPlaylists = { navigate(Route.Playlists) },
                    onOpenLocalSongs = { navigate(Route.LocalSongs) },
                    onOpenTogether = { navigate(Route.Together) },
                    onOpenQueue = { navigate(Route.Queue) },
                    onToggleTheme = {
                        isDark = !isDark
                        storage.save(Settings::darkMode, isDark)
                    },
                    onQuit = onQuit,
                )
            }

            is Route.Help -> HelpScreen(onGoBack = goBack)

            is Route.Search -> SearchScreen(
                searchViewModel = searchViewModel,
                playerViewModel = playerViewModel,
                onGoBack = goBack,
            )

            is Route.Login -> LoginScreen(
                authViewModel = authViewModel,
                onGoBack = goBack,
            )

            is Route.LogViewer -> LogViewerScreen(
                onGoBack = goBack,
            )

            is Route.Playlists -> PlaylistScreen(
                playlistViewModel = playlistViewModel,
                playerViewModel = playerViewModel,
                onGoBack = goBack,
            )

            is Route.LocalSongs -> LocalScreen(
                localViewModel = localViewModel,
                playerViewModel = playerViewModel,
                onGoBack = goBack,
            )

            is Route.Together -> TogetherScreen(
                channelViewModel = channelViewModel,
                onGoBack = goBack,
            )

            is Route.Queue -> QueueScreen(
                playerState = playerState,
                playerViewModel = playerViewModel,
                onGoBack = goBack,
            )
        }
    }

    // 组合根按无界约束测量，全屏应用必须自己声明画布；尺寸随终端 resize 重组更新。
    Box(
        modifier = Modifier
            .size(localTerminal.size.columns, localTerminal.size.rows)
            .background(palette.mainBg)
    ) {
        CompositionLocalProvider(
            LocalTuiColors provides palette,
            LocalGlobalActions provides appGlobalActions,
        ) {
            Column {
                // Header
                val channelLabel =
                    (togetherState.channelState as? com.eimsound.eimusic.together.ChannelState.Joined)?.let {
                        "${it.channel.name.render()}(${it.channel.members.size})"
                    }
                HeaderBar(
                    "EIMusic",
                    playerState,
                    authViewModel,
                    isDark,
                    onNavigate = { navigate(Route.Login) },
                    channelLabel = channelLabel
                )
                BoxWithConstraints(Modifier.weight(1f).onKeyEvent { event ->
                    // 弹窗打开时全局快捷键停用，按键只交给弹窗。
                    if (overlayRoute) false else dispatchGlobalKey(event, appGlobalActions)
                }) {
                    // 内容区真实可用尺寸来自父级约束，不再用终端行列数硬减。
                    CompositionLocalProvider(
                        LocalSafeContentSize provides SafeContentSize(maxWidth, maxHeight),
                    ) {
                        if (baseRoute != null) {
                            CompositionLocalProvider(LocalUiInputEnabled provides false) {
                                RouteContent(baseRoute)
                            }
                        }
                        CompositionLocalProvider(LocalUiInputEnabled provides true) {
                            RouteContent(currentRoute)
                        }
                    }
                }

                // 底部页面 tabs；点击切换时回到该页面的根路由（同时关闭弹窗）。
                TabBar(
                    tabs = tabRoutes.map { (label, route) ->
                        TabItem(
                            label = label,
                            selected = activeTabRoute == route,
                            onClick = {
                                if (activeTabRoute != route) {
                                    backStack = listOf(route)
                                }
                            },
                        )
                    },
                )
            }

        }
    }
}

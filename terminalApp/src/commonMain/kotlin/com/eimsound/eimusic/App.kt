package com.eimsound.eimusic

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.GlobalKeyActions
import com.eimsound.eimusic.keyboard.LocalGlobalActions
import com.eimsound.eimusic.keyboard.dispatchGlobalKey
import com.eimsound.eimusic.navigation.Route
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.db.DbSessionRepository
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.ui.components.HeaderBar
import com.eimsound.eimusic.ui.components.LocalStatusBarHint
import com.eimsound.eimusic.ui.screens.HelpDialog
import com.eimsound.eimusic.ui.screens.LocalScreen
import com.eimsound.eimusic.ui.screens.LogViewerScreen
import com.eimsound.eimusic.ui.screens.LoginDialog
import com.eimsound.eimusic.ui.screens.MainScreen
import com.eimsound.eimusic.ui.screens.PlaylistScreen
import com.eimsound.eimusic.ui.screens.QueueScreen
import com.eimsound.eimusic.ui.screens.SearchPage
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
    initialTab: Int,
    sessionRepository: DbSessionRepository,
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
    val statusHint by LocalStatusBarHint.current

    val initialRoute = when (initialTab) {
        1 -> Route.Playlists
        2 -> Route.LocalSongs
        3 -> Route.Together
        4 -> Route.Queue
        else -> Route.Main
    }
    var backStack by remember { mutableStateOf(listOf(initialRoute)) }
    val playerState by playerViewModel.state.bridgeToMutable()
    val togetherState by channelViewModel.uiState.bridgeToMutable()
    val currentRoute = backStack.last()

    // 弹窗类路由：保留底层页面绘制，弹窗叠在上面。
    val overlayRoute = currentRoute.isOverlay
    val baseRoute = if (overlayRoute) backStack.getOrNull(backStack.size - 2) else null

    val navigate: (Route) -> Unit = { route -> backStack = backStack + route }
    val goBack: () -> Unit = { if (backStack.size > 1) backStack = backStack.dropLast(1) }

    // 底部标签页：只放可切换的页面，弹窗类路由不算标签。
    val tabRoutes = listOf(
        "1 Home" to Route.Main,
        "2 Playlists" to Route.Playlists,
        "3 Local" to Route.LocalSongs,
        "4 Together" to Route.Together,
        "5 Queue" to Route.Queue,
        "6 Search" to Route.Search(),
    )
    val activeTabRoute = baseRoute ?: currentRoute

    LaunchedEffect(activeTabRoute) {
        val index = tabRoutes.indexOfFirst { it.second == activeTabRoute }
        if (index >= 0) sessionRepository.saveTab(index)
    }

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
            onToggleMute = { playerViewModel.toggleMute() },
            onOpenSearch = { backStack = listOf(Route.Search()) },
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
                    onOpenSearch = { backStack = listOf(Route.Search()) },
                    onOpenLogin = { navigate(Route.Login) },
                    onOpenLogs = { navigate(Route.LogViewer) },
                    onToggleTheme = {
                        isDark = !isDark
                        storage.save(Settings::darkMode, isDark)
                    },
                    onQuit = onQuit,
                )
            }

            // 弹层路由只通过 OverlayHost 的 overlay 槽渲染，不作为 base 页面。
            is Route.Help, is Route.Login -> Unit

            is Route.Search -> SearchPage(
                searchViewModel = searchViewModel,
                playerViewModel = playerViewModel,
                playlistViewModel = playlistViewModel,
                onGoBack = { backStack = listOf(Route.Main) },
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
                playlistViewModel = playlistViewModel,
                onGoBack = goBack,
            )
        }
    }

    // 组合根按无界约束测量，全屏应用必须自己声明画布；尺寸随终端 resize 重组更新。
    @Composable
    fun OverlayRouteContent(route: Route) {
        when (route) {
            is Route.Help -> HelpDialog(onGoBack = goBack)
            is Route.Login -> LoginDialog(
                authViewModel = authViewModel,
                onGoBack = goBack,
            )
            else -> Unit
        }
    }

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
                    onOpenQueue = { if (currentRoute !is Route.Queue) navigate(Route.Queue) },
                    onOpenTogether = { if (currentRoute !is Route.Together) navigate(Route.Together) },
                    onToggleTheme = {
                        isDark = !isDark
                        storage.save(Settings::darkMode, isDark)
                    },
                    channelLabel = channelLabel,
                )
                BoxWithConstraints(Modifier.weight(1f).onKeyEvent { event ->
                    // Ctrl-C 是全局退出，任何页面/控件都应生效。
                    if (event.ctrl && event.key == "c") {
                        onQuit()
                        return@onKeyEvent true
                    }
                    // 数字 1-5 直达标签页；Tab / Shift+Tab 循环切换。
                    // 输入框会先消费字符，所以在输入时数字仍然是正常输入。
                    val digit = event.key.toIntOrNull()
                    if (digit != null && digit in 1..tabRoutes.size && !event.ctrl && !event.alt) {
                        val target = tabRoutes[digit - 1].second
                        if (overlayRoute || activeTabRoute != target) {
                            backStack = listOf(target)
                        }
                        true
                    } else if (event.key == "Tab" && !event.ctrl && !event.alt) {
                        val currentIndex = tabRoutes.indexOfFirst { it.second == activeTabRoute }
                        val base = if (currentIndex >= 0) currentIndex else 0
                        val delta = if (event.shift) -1 else 1
                        val next = (base + delta + tabRoutes.size) % tabRoutes.size
                        backStack = listOf(tabRoutes[next].second)
                        true
                    } else if (overlayRoute) {
                        // 弹窗打开时除切页外，全局快捷键停用。
                        false
                    } else {
                        dispatchGlobalKey(event, appGlobalActions)
                    }
                }) {
                    // 内容区真实可用尺寸来自父级约束，不再用终端行列数硬减。
                    CompositionLocalProvider(
                        LocalSafeContentSize provides SafeContentSize(maxWidth, maxHeight),
                    ) {
                        OverlayHost(
                            overlay = if (overlayRoute && baseRoute != null) {
                                { OverlayRouteContent(currentRoute) }
                            } else {
                                null
                            },
                        ) {
                            // 库的 modal 门禁已经挡住 base 输入；这里再用 local 冻结
                            // ScreenFrame 的 hint/按键，保证弹窗关闭后 hint 会重算。
                            CompositionLocalProvider(LocalUiInputEnabled provides !overlayRoute) {
                                RouteContent(baseRoute ?: currentRoute)
                            }
                        }
                    }
                }

                // 底部页面 tabs；点击切换时回到该页面的根路由（同时关闭弹窗）。
                TabRow(
                    tabs = tabRoutes.map { it.first },
                    selectedIndex = tabRoutes.indexOfFirst { it.second == activeTabRoute },
                    onSelect = { index ->
                        val route = tabRoutes[index].second
                        if (overlayRoute || activeTabRoute != route) {
                            backStack = listOf(route)
                        }
                    },
                    colors = TabRowColors(
                        container = palette.statusBarBg,
                        selectedBackground = palette.selectionBg,
                        selectedContent = palette.highlightFg,
                        content = palette.statusBarFg,
                        hoverBackground = palette.hoverBg,
                        divider = palette.dimBorderFg,
                    ),
                    trailing = if (statusHint.isBlank()) {
                        null
                    } else {
                        {
                            Text(
                                "  $statusHint",
                                modifier = Modifier.weight(1f),
                                color = palette.statusBarFg,
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                )
            }

        }
    }
}

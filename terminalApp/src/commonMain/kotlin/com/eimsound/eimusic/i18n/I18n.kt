package com.eimsound.eimusic.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** UI 语言。 */
enum class Language(val code: String) {
    English("en"),
    Chinese("zh"),
    ;

    companion object {
        fun fromCode(code: String): Language =
            entries.firstOrNull { it.code == code } ?: Chinese
    }
}

/**
 * 轻量 i18n：键值表 + 可观察当前语言。
 *
 * 在 Compose 中读取 [t] 会订阅语言状态，切换语言后自动重组；
 * 非 Composable 的状态栏提示函数也可以直接调用。
 */
object I18n {
    var language by mutableStateOf(Language.Chinese)
        private set

    fun setLanguage(language: Language) {
        this.language = language
    }

    fun setLanguage(code: String) {
        language = Language.fromCode(code)
    }

    operator fun get(key: String): String =
        tables[language]?.get(key) ?: tables[Language.English]?.get(key) ?: key

    fun t(key: String, vararg args: Any?): String {
        val template = get(key)
        if (args.isEmpty()) return template
        return args.foldIndexed(template) { index, acc, arg ->
            acc.replace("{$index}", arg.toString())
        }
    }

    private val tables: Map<Language, Map<String, String>> = mapOf(
        Language.English to english,
        Language.Chinese to chinese,
    )
}

fun t(key: String, vararg args: Any?): String = I18n.t(key, *args)

/** 按键说明（KeyBinding.description）的中文映射；英文直接返回原文。 */
fun tDesc(description: String): String =
    if (I18n.language == Language.Chinese) descriptionZh[description] ?: description else description

private val descriptionZh: Map<String, String> = mapOf(
    "Add" to "添加",
    "Add last" to "添加到末尾",
    "Add next" to "下一首播放",
    "Add path" to "添加路径",
    "Back" to "返回",
    "Back to input" to "返回输入",
    "Cancel" to "取消",
    "Chat" to "聊天",
    "Clear" to "清空",
    "Confirm" to "确认",
    "Current" to "当前",
    "Cursor down" to "光标下移",
    "Cursor left" to "光标左移",
    "Cursor right" to "光标右移",
    "Cursor up" to "光标上移",
    "Custom channel" to "自定义频道",
    "Delete" to "删除",
    "Delete forward" to "删除后一个",
    "Delete path" to "删除路径",
    "Join" to "加入",
    "Leave" to "离开",
    "Login" to "登录",
    "Manage paths" to "管理路径",
    "Menu" to "菜单",
    "Move down" to "下移",
    "Move up" to "上移",
    "New local" to "新建本地",
    "Next track" to "下一首",
    "Open / edit" to "打开 / 编辑",
    "Play" to "播放",
    "Play / Pause" to "播放 / 暂停",
    "Previous track" to "上一首",
    "Quit" to "退出",
    "Refresh" to "刷新",
    "Remove" to "移除",
    "Search music" to "搜索音乐",
    "Seek +5s" to "快进 5 秒",
    "Seek -5s" to "后退 5 秒",
    "Settings" to "设置",
    "Toggle play mode" to "切换播放模式",
    "Toggle theme" to "切换主题",
    "Toggle this help" to "打开 / 关闭帮助",
    "Toggle track list" to "切换歌曲列表",
    "View logs" to "查看日志",
)

private val english: Map<String, String> = mapOf(
    // Tabs
    "tab.home" to "Home",
    "tab.playlists" to "Playlists",
    "tab.local" to "Local",
    "tab.together" to "Together",
    "tab.queue" to "Queue",
    "tab.search" to "Search",

    // Common
    "common.back" to "Back",
    "common.cancel" to "Cancel",
    "common.confirm" to "Confirm",
    "common.loading" to "Loading...",
    "common.error" to "Error: {0}",
    "common.confirmHint" to "Enter to confirm  ·  Esc to cancel",

    // Settings
    "settings.title" to "Settings",
    "settings.subtitle" to "Terminal settings",
    "settings.appearance" to "Appearance",
    "settings.network" to "Network",
    "settings.sources" to "Music sources",
    "settings.about" to "About",
    "settings.theme" to "Theme",
    "settings.theme.dark" to "Dark",
    "settings.theme.light" to "Light",
    "settings.language" to "Language",
    "settings.language.en" to "English",
    "settings.language.zh" to "Chinese",
    "settings.serverHttp" to "Server HTTP",
    "settings.serverWs" to "Server WS",
    "settings.proxy" to "Proxy",
    "settings.proxy.on" to "On",
    "settings.proxy.off" to "Off",
    "settings.proxyAddress" to "Proxy address",
    "settings.sourcesConfig" to "Source config",
    "settings.sourcesConfigured" to "{0} configured",
    "settings.version" to "Version",
    "settings.update" to "Check for updates",
    "settings.update.idle" to "Not checked",
    "settings.update.checking" to "Checking...",
    "settings.update.upToDate" to "Up to date (v{0})",
    "settings.update.available" to "v{0} available",
    "settings.update.failed" to "Check failed",
    "settings.update.download" to "Download",
    "settings.edit" to "Edit {0}",
    "settings.current" to "Current: {0}",
    "settings.placeholder" to "Enter new value, Enter to save",
    "settings.saved" to "Saved, restart to apply",
    "settings.sourceUrls" to "{0} URLs",
    "settings.sourceUnset" to "Not configured",
    "settings.proxyHint" to "host:port",
    "settings.sourcesHint" to "Comma separated",
    "source.netease" to "NetEase",
    "source.spotify" to "Spotify",
    "source.qq" to "QQ Music",
    "source.xiami" to "Xiami",
    "source.kuwo" to "Kuwo",
    "source.kugou" to "Kugou",
    "source.baidu" to "Baidu",
    "source.youtube" to "YouTube",
    "source.youtubeMusic" to "YouTube Music",

    // Status bar hints
    "hint.settings.main" to "j/k:Select  Enter:Open  Esc:Back",
    "hint.settings.sources" to "j/k:Select  Enter:Edit  Esc:Back",
    "hint.settings.editing" to "Enter:Save  Esc:Cancel",
    "hint.local.songs" to "j/k:Scroll  Enter:Play  r:Refresh  /:ManagePaths  m:Menu  Esc:Back",
    "hint.local.paths" to "j/k:Scroll  d:Delete  a:Add  /:Toggle  Esc:Back",
    "hint.playlist.list" to "j/k:Scroll  Enter:Open  a:Add  n:New  d:Delete  Tab:Open  Esc:Back",
    "hint.playlist.tracks" to "j/k:Scroll  Enter:Play  K/J:Move  d:Remove  r:Refresh  m:Menu",
    "hint.search.input" to "Enter:Search  ↓:History",
    "hint.search.history" to "↑/↓:Select  Enter:Search",
    "hint.search.results" to "j/k:Move  Enter:Play  m:Menu",
    "hint.together.welcome" to "j:Join  c:Custom  l:Leave  Esc:Back",
    "hint.together.messages" to "j/k:Scroll  m:Chat  l:Leave  Esc:Back",
    "hint.together.chat" to "Enter:Send  Esc:Cancel",
    "hint.together.channel" to "Enter:Join  Esc:Cancel",
    "hint.help" to "Esc:Close",
    "hint.input" to "Enter:Confirm  Esc:Cancel",
    "hint.main.help" to "Help",
    "hint.main.playPause" to "Play/Pause",
    "hint.main.prev" to "Prev",
    "hint.main.next" to "Next",
    "hint.main.search" to "Search",
    "hint.main.quit" to "Quit",
    "hint.queue" to "j/k:Scroll  Enter:Play  K/J:Move  g:Current  m:Menu  d:Remove  c:Clear  Esc:Back",
    "hint.logviewer" to "j/k:Scroll  c:Clear  Esc:Close",
    "hint.listview" to "j/k:Scroll  Enter:Confirm",
    "hint.tracktable" to "j/k:Scroll  Enter:Play  m:Menu",

    // Local
    "local.title.songs" to "Local Songs ({0})",
    "local.title.paths" to "Local Paths ({0})",
    "local.title.addPath" to "Add Path",
    "local.managePaths" to "Manage paths",
    "local.addPath" to "+ Add path",
    "local.done" to "Done",
    "local.enterPath" to "Enter directory path:",
    "local.placeholder" to "/home/user/Music",
    "local.empty" to "No local songs found.",
    "local.emptyHint" to "Click 'Manage paths' to add a music directory.",
    "local.scanning" to "Scanning...",
    "local.noPaths" to "No paths configured.",
    "local.noPathsHint" to "Press 'a' to add a music directory.",
    "local.pathHeader" to "Path",

    // Playlists
    "playlist.title" to "My Playlists",
    "playlist.newLocal" to "+ New local",
    "playlist.importUrl" to "+ Import URL",
    "playlist.namePlaceholder" to "My playlist",
    "playlist.urlPlaceholder" to "https://music.163.com/playlist/...",
    "playlist.enterName" to "Enter local playlist name:",
    "playlist.enterUrl" to "Enter playlist URL:",
    "playlist.empty" to "No playlists saved.",
    "playlist.emptyHint" to "Press 'a' to import, 'n' or + New local to create.",
    "playlist.loading" to "Loading...",
    "playlist.tracksTitle" to "{0} · tracks",
    "playlist.tracksSuffix" to "tracks",
    "playlist.emptyTracks" to "No tracks in this playlist.",
    "playlist.removeTrack" to "Remove from playlist",
    "playlist.name" to "Name",
    "playlist.tracks" to "Tracks",

    // Search
    "search.title" to "Search",
    "search.placeholder" to "Type and press Enter to search",
    "search.history" to "History",
    "search.results" to "Results",
    "search.searching" to "Searching...",

    // Queue
    "queue.title" to "Queue",
    "queue.remove" to "Remove from queue",
    "queue.empty" to "Queue is empty.",

    // Main
    "main.loading" to "Loading...",
    "main.noTrack" to "No track playing",
    "main.recent" to "Recent",
    "main.unknown" to "Unknown",
    "main.unknownArtist" to "Unknown Artist",
    "main.searchHint" to "Press 'f' to search  •  1-6 switch tabs",

    // Login
    "login.title" to "Login",
    "login.usernamePlaceholder" to "Enter username",
    "login.notLoggedIn" to "Not logged in",
    "login.loggedInAs" to "Logged in as {0}",
    "login.loggingIn" to "Logging in...",
    "login.confirm" to "Enter to login  |  Esc to cancel",

    // Help
    "help.title" to "Keyboard Controls",

    // Track info
    "trackInfo.title" to "Track info",
    "trackInfo.titleLabel" to "Title:",
    "trackInfo.artist" to "Artist:",
    "trackInfo.album" to "Album:",
    "trackInfo.source" to "Source:",
    "trackInfo.id" to "Id:",
    "trackInfo.uri" to "URI:",

    // Context menu
    "menu.addToPlaylist" to "Add to playlist",
    "menu.playNext" to "Play next",
    "menu.trackInfo" to "Track info",
    "picker.title" to "Add to local playlist",
    "picker.empty" to "No local playlists.",
    "header.login" to "login",

    // Track table
    "table.title" to "Title",
    "table.artist" to "Artist",
    "table.duration" to "Duration",
    "table.empty" to "-- empty --",

    // Logs
    "logs.title" to "Logs (Esc to close, j/k to scroll)",

    // Together
    "together.welcomeSubtitle" to "Listen together in real-time",
    "together.join" to "Join default channel",
    "together.leave" to "Leave / Disconnect",
    "together.back" to "Back",
    "together.emptyMessages" to "No messages yet.",
    "together.emptyHint" to "Press 'm' to start chatting.",
    "together.chatPlaceholder" to "Type a message and press Enter...",
    "together.enterChannel" to "Enter channel ID:",
    "together.connected" to "Connected",
    "together.connecting" to "Connecting...",
    "together.disconnected" to "Disconnected",
    "together.reconnecting" to "Reconnecting...",
    "together.confirmJoin" to "Enter to join  ·  Esc to cancel",
    "together.play" to "▶ played",
    "together.pause" to "⏸ paused",
    "together.changeTrack" to "🎵 changed to {0}",
    "together.seek" to "⏩ seeked",
    "together.next" to "⏭ skipped",
    "together.previous" to "⏮ went back",
    "together.playMode" to "🔄 switched mode",
    "together.controlled" to "⚡ controlled",
)

private val chinese: Map<String, String> = mapOf(
    // Tabs
    "tab.home" to "主页",
    "tab.playlists" to "歌单",
    "tab.local" to "本地",
    "tab.together" to "一起听",
    "tab.queue" to "队列",
    "tab.search" to "搜索",

    // Common
    "common.back" to "返回",
    "common.cancel" to "取消",
    "common.confirm" to "确认",
    "common.loading" to "加载中...",
    "common.error" to "错误：{0}",
    "common.confirmHint" to "回车确认  ·  Esc 取消",

    // Settings
    "settings.title" to "设置",
    "settings.subtitle" to "终端设置",
    "settings.appearance" to "外观",
    "settings.network" to "网络",
    "settings.sources" to "音源",
    "settings.about" to "关于",
    "settings.theme" to "主题",
    "settings.theme.dark" to "暗色",
    "settings.theme.light" to "亮色",
    "settings.language" to "语言",
    "settings.language.en" to "英文",
    "settings.language.zh" to "中文",
    "settings.serverHttp" to "服务器 HTTP",
    "settings.serverWs" to "服务器 WS",
    "settings.proxy" to "代理",
    "settings.proxy.on" to "开启",
    "settings.proxy.off" to "关闭",
    "settings.proxyAddress" to "代理地址",
    "settings.sourcesConfig" to "音源配置",
    "settings.sourcesConfigured" to "{0} 个已配置",
    "settings.version" to "版本",
    "settings.update" to "检查更新",
    "settings.update.idle" to "未检查",
    "settings.update.checking" to "检查中...",
    "settings.update.upToDate" to "已是最新（v{0}）",
    "settings.update.available" to "发现新版本 v{0}",
    "settings.update.failed" to "检查失败",
    "settings.update.download" to "下载地址",
    "settings.edit" to "编辑 {0}",
    "settings.current" to "当前：{0}",
    "settings.placeholder" to "输入新值，回车保存",
    "settings.saved" to "已保存，重启后生效",
    "settings.sourceUrls" to "{0} 个地址",
    "settings.sourceUnset" to "未配置",
    "settings.proxyHint" to "host:port",
    "settings.sourcesHint" to "逗号分隔",
    "source.netease" to "网易云",
    "source.spotify" to "Spotify",
    "source.qq" to "QQ 音乐",
    "source.xiami" to "虾米",
    "source.kuwo" to "酷我",
    "source.kugou" to "酷狗",
    "source.baidu" to "百度",
    "source.youtube" to "YouTube",
    "source.youtubeMusic" to "YouTube Music",

    // Status bar hints
    "hint.settings.main" to "j/k:选择  Enter:打开  Esc:返回",
    "hint.settings.sources" to "j/k:选择  Enter:编辑  Esc:返回",
    "hint.settings.editing" to "Enter:保存  Esc:取消",
    "hint.local.songs" to "j/k:滚动  Enter:播放  r:刷新  /:管理路径  m:菜单  Esc:返回",
    "hint.local.paths" to "j/k:滚动  d:删除  a:添加  /:切换  Esc:返回",
    "hint.playlist.list" to "j/k:滚动  Enter:打开  a:导入  n:新建  d:删除  Tab:打开  Esc:返回",
    "hint.playlist.tracks" to "j/k:滚动  Enter:播放  K/J:移动  d:移除  r:刷新  m:菜单",
    "hint.search.input" to "Enter:搜索  ↓:历史",
    "hint.search.history" to "↑/↓:选择  Enter:搜索",
    "hint.search.results" to "j/k:移动  Enter:播放  m:菜单",
    "hint.together.welcome" to "j:加入  c:自定义  l:离开  Esc:返回",
    "hint.together.messages" to "j/k:滚动  m:聊天  l:离开  Esc:返回",
    "hint.together.chat" to "Enter:发送  Esc:取消",
    "hint.together.channel" to "Enter:加入  Esc:取消",
    "hint.help" to "Esc:关闭",
    "hint.input" to "Enter:确认  Esc:取消",
    "hint.main.help" to "帮助",
    "hint.main.playPause" to "播放/暂停",
    "hint.main.prev" to "上一首",
    "hint.main.next" to "下一首",
    "hint.main.search" to "搜索",
    "hint.main.quit" to "退出",
    "hint.queue" to "j/k:滚动  Enter:播放  K/J:移动  g:当前  m:菜单  d:移除  c:清空  Esc:返回",
    "hint.logviewer" to "j/k:滚动  c:清空  Esc:关闭",
    "hint.listview" to "j/k:滚动  Enter:确认",
    "hint.tracktable" to "j/k:滚动  Enter:播放  m:菜单",

    // Local
    "local.title.songs" to "本地歌曲 ({0})",
    "local.title.paths" to "本地路径 ({0})",
    "local.title.addPath" to "添加路径",
    "local.managePaths" to "管理路径",
    "local.addPath" to "+ 添加路径",
    "local.done" to "完成",
    "local.enterPath" to "输入目录路径：",
    "local.placeholder" to "/home/user/Music",
    "local.empty" to "未找到本地歌曲。",
    "local.emptyHint" to "点击“管理路径”添加音乐目录。",
    "local.scanning" to "扫描中...",
    "local.noPaths" to "尚未配置路径。",
    "local.noPathsHint" to "按 a 添加音乐目录。",
    "local.pathHeader" to "路径",

    // Playlists
    "playlist.title" to "我的歌单",
    "playlist.newLocal" to "+ 新建本地",
    "playlist.importUrl" to "+ 导入链接",
    "playlist.namePlaceholder" to "我的歌单",
    "playlist.urlPlaceholder" to "https://music.163.com/playlist/...",
    "playlist.enterName" to "输入本地歌单名称：",
    "playlist.enterUrl" to "输入歌单链接：",
    "playlist.empty" to "暂无歌单。",
    "playlist.emptyHint" to "按 a 导入，n 或 + 新建本地 创建。",
    "playlist.loading" to "加载中...",
    "playlist.tracksTitle" to "{0} · 歌曲",
    "playlist.tracksSuffix" to "歌曲",
    "playlist.emptyTracks" to "此歌单暂无歌曲。",
    "playlist.removeTrack" to "从歌单移除",
    "playlist.name" to "名称",
    "playlist.tracks" to "歌曲",

    // Search
    "search.title" to "搜索",
    "search.placeholder" to "输入后按 Enter 搜索",
    "search.history" to "历史",
    "search.results" to "结果",
    "search.searching" to "搜索中...",

    // Queue
    "queue.title" to "队列",
    "queue.remove" to "从队列移除",
    "queue.empty" to "队列为空。",

    // Main
    "main.loading" to "加载中...",
    "main.noTrack" to "未在播放",
    "main.recent" to "最近播放",
    "main.unknown" to "未知",
    "main.unknownArtist" to "未知艺术家",
    "main.searchHint" to "按 f 搜索  •  1-6 切换页",

    // Login
    "login.title" to "登录",
    "login.usernamePlaceholder" to "输入用户名",
    "login.notLoggedIn" to "未登录",
    "login.loggedInAs" to "已登录为 {0}",
    "login.loggingIn" to "登录中...",
    "login.confirm" to "Enter 登录  |  Esc 取消",

    // Help
    "help.title" to "键盘操作",

    // Track info
    "trackInfo.title" to "歌曲信息",
    "trackInfo.titleLabel" to "标题：",
    "trackInfo.artist" to "艺术家：",
    "trackInfo.album" to "专辑：",
    "trackInfo.source" to "来源：",
    "trackInfo.id" to "ID：",
    "trackInfo.uri" to "URI：",

    // Context menu
    "menu.addToPlaylist" to "添加到歌单",
    "menu.playNext" to "下一首播放",
    "menu.trackInfo" to "歌曲信息",
    "picker.title" to "添加到本地歌单",
    "picker.empty" to "暂无本地歌单。",
    "header.login" to "登录",

    // Track table
    "table.title" to "标题",
    "table.artist" to "艺术家",
    "table.duration" to "时长",
    "table.empty" to "-- 空 --",

    // Logs
    "logs.title" to "日志（Esc 关闭，j/k 滚动）",

    // Together
    "together.welcomeSubtitle" to "一起实时听歌",
    "together.join" to "加入默认频道",
    "together.leave" to "离开 / 断开",
    "together.back" to "返回",
    "together.emptyMessages" to "暂无消息。",
    "together.emptyHint" to "按 m 开始聊天。",
    "together.chatPlaceholder" to "输入消息，回车发送...",
    "together.enterChannel" to "输入频道 ID：",
    "together.connected" to "已连接",
    "together.connecting" to "连接中...",
    "together.disconnected" to "未连接",
    "together.reconnecting" to "重连中...",
    "together.confirmJoin" to "回车加入  ·  Esc 取消",
    "together.play" to "▶ 播放",
    "together.pause" to "⏸ 暂停",
    "together.changeTrack" to "🎵 切换到 {0}",
    "together.seek" to "⏩ 跳转",
    "together.next" to "⏭ 下一首",
    "together.previous" to "⏮ 上一首",
    "together.playMode" to "🔄 切换模式",
    "together.controlled" to "⚡ 控制",
)

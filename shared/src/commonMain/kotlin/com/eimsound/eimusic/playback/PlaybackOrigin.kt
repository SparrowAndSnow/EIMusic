package com.eimsound.eimusic.playback

import com.eimsound.eimusic.events.EventSource

/** 播放命令来源，用于决定事件是否需要回传到 Together。 */
enum class PlaybackOrigin {
    LocalUi,
    RemoteSync,
    System,
    InternalAutoAdvance,
}

fun PlaybackOrigin.toEventSource(): EventSource = when (this) {
    PlaybackOrigin.LocalUi -> EventSource.User
    PlaybackOrigin.RemoteSync -> EventSource.Remote
    PlaybackOrigin.System -> EventSource.System
    PlaybackOrigin.InternalAutoAdvance -> EventSource.Player
}

fun EventSource.toPlaybackOrigin(): PlaybackOrigin = when (this) {
    EventSource.User -> PlaybackOrigin.LocalUi
    EventSource.Remote -> PlaybackOrigin.RemoteSync
    EventSource.System -> PlaybackOrigin.System
    else -> PlaybackOrigin.InternalAutoAdvance
}

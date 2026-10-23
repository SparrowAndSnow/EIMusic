package com.eimsound.eimusic.keyboard

import com.jakewharton.mosaic.layout.KeyEvent

// ── Focus ──

enum class PlaylistFocus { List, TrackList, Input }

// ── Screen-specific actions ──

sealed interface PlaylistAction : KeyAction {
    data object AddPlaylist : PlaylistAction
    data object CreateLocal : PlaylistAction
    data object DeletePlaylist : PlaylistAction
    data object RemoveTrack : PlaylistAction
    data object MoveTrackUp : PlaylistAction
    data object MoveTrackDown : PlaylistAction
    data object ToggleTrackList : PlaylistAction
    data object GoBack : PlaylistAction
}

val playlistKeyBindings: List<KeyBinding<PlaylistAction>> = listOf(
    KeyBinding(listOf("a"), "a", PlaylistAction.AddPlaylist, "Add"),
    KeyBinding(listOf("n"), "n", PlaylistAction.CreateLocal, "New local"),
    KeyBinding(listOf("d"), "d", PlaylistAction.DeletePlaylist, "Delete"),
    KeyBinding(listOf("K"), "K", PlaylistAction.MoveTrackUp, "Move up"),
    KeyBinding(listOf("J"), "J", PlaylistAction.MoveTrackDown, "Move down"),
    KeyBinding(listOf("Tab"), "Tab", PlaylistAction.ToggleTrackList, "Toggle track list"),
    KeyBinding(listOf("Escape"), "Esc", PlaylistAction.GoBack, "Back"),
)

private val playlistKeyMap: Map<String, PlaylistAction> = buildMap {
    playlistKeyBindings.forEach { b -> b.keys.forEach { k -> put(k, b.action) } }
}

fun mapPlaylistKey(key: String): PlaylistAction? = playlistKeyMap[key]

// ── Dispatch ──

/** Playlists 页面按键回调集合。 */
data class PlaylistKeyActions(
    val setShowTrackList: (Boolean) -> Unit,
    val setFocus: (PlaylistFocus) -> Unit,
    val goBack: () -> Unit,
    val beginImport: () -> Unit,
    val beginLocal: () -> Unit,
    val removePlaylist: () -> Unit,
    val removeTrack: () -> Unit,
    val moveTrackUp: () -> Unit,
    val moveTrackDown: () -> Unit,
    val selectPlaylist: () -> Unit,
)

fun dispatchPlaylistKey(
    event: KeyEvent,
    focus: PlaylistFocus,
    showTrackList: Boolean,
    actions: PlaylistKeyActions,
): Boolean {
    if (event.ctrl || event.alt) return false
    return when (focus) {
    PlaylistFocus.Input -> false // InputField handles its own keys

    PlaylistFocus.List -> {
        val screenAction = mapPlaylistKey(event.key)
        when (screenAction) {
            PlaylistAction.AddPlaylist -> { actions.beginImport(); true }
            PlaylistAction.CreateLocal -> { actions.beginLocal(); true }
            PlaylistAction.DeletePlaylist -> { actions.removePlaylist(); true }
            PlaylistAction.ToggleTrackList -> { actions.selectPlaylist(); actions.setShowTrackList(true); actions.setFocus(PlaylistFocus.TrackList); true }
            PlaylistAction.GoBack -> { actions.goBack(); true }
            else -> false // ListView handles its own keys via onConfirm
        }
    }

    PlaylistFocus.TrackList -> {
        val screenAction = mapPlaylistKey(event.key)
        when (screenAction) {
            PlaylistAction.GoBack -> { actions.setShowTrackList(false); actions.setFocus(PlaylistFocus.List); true }
            PlaylistAction.DeletePlaylist -> { actions.removeTrack(); true }
            PlaylistAction.MoveTrackUp -> { actions.moveTrackUp(); true }
            PlaylistAction.MoveTrackDown -> { actions.moveTrackDown(); true }
            else -> false // TrackTable handles its own keys
        }
    }
    }
}

fun playlistStatusBarHint(focus: PlaylistFocus): String = when (focus) {
    PlaylistFocus.Input -> inputFieldStatusBarHint()
    PlaylistFocus.List -> "j/k:Scroll  Enter:Open  a:Add  n:New  d:Delete  Tab:Open  Esc:Back"
    PlaylistFocus.TrackList -> "j/k:Scroll  Enter:Play  p:PlayAll  K/J:Move  d:Remove  Esc:Back"
}

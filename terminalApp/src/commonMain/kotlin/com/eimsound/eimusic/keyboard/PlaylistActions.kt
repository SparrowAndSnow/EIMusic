package com.eimsound.eimusic.keyboard

import com.jakewharton.mosaic.layout.KeyEvent

// ── Focus ──

enum class PlaylistFocus { List, TrackList, Input }

// ── Screen-specific actions ──

sealed interface PlaylistAction : KeyAction {
    data object AddPlaylist : PlaylistAction
    data object DeletePlaylist : PlaylistAction
    data object ToggleTrackList : PlaylistAction
    data object GoBack : PlaylistAction
}

val playlistKeyBindings: List<KeyBinding<PlaylistAction>> = listOf(
    KeyBinding(listOf("a"), "a", PlaylistAction.AddPlaylist, "Add"),
    KeyBinding(listOf("d"), "d", PlaylistAction.DeletePlaylist, "Delete"),
    KeyBinding(listOf("Tab"), "Tab", PlaylistAction.ToggleTrackList, "Toggle track list"),
    KeyBinding(listOf("Escape"), "Esc", PlaylistAction.GoBack, "Back"),
)

private val playlistKeyMap: Map<String, PlaylistAction> = buildMap {
    playlistKeyBindings.forEach { b -> b.keys.forEach { k -> put(k, b.action) } }
}

fun mapPlaylistKey(key: String): PlaylistAction? = playlistKeyMap[key]

// ── Dispatch ──

fun dispatchPlaylistKey(
    event: KeyEvent,
    focus: PlaylistFocus,
    showTrackList: Boolean,
    onSetShowTrackList: (Boolean) -> Unit,
    onSetFocus: (PlaylistFocus) -> Unit,
    onGoBack: () -> Unit,
    onAddPlaylist: () -> Unit,
    onRemovePlaylist: () -> Unit,
    onSelectPlaylist: () -> Unit,
): Boolean = when (focus) {
    PlaylistFocus.Input -> false // InputField handles its own keys

    PlaylistFocus.List -> {
        val screenAction = mapPlaylistKey(event.key)
        when (screenAction) {
            PlaylistAction.AddPlaylist -> { onSetFocus(PlaylistFocus.Input); true }
            PlaylistAction.DeletePlaylist -> { onRemovePlaylist(); true }
            PlaylistAction.ToggleTrackList -> { onSelectPlaylist(); onSetShowTrackList(true); onSetFocus(PlaylistFocus.TrackList); true }
            PlaylistAction.GoBack -> { onGoBack(); true }
            else -> false // ListView handles its own keys via onConfirm
        }
    }

    PlaylistFocus.TrackList -> {
        val screenAction = mapPlaylistKey(event.key)
        when (screenAction) {
            PlaylistAction.GoBack -> { onSetShowTrackList(false); onSetFocus(PlaylistFocus.List); true }
            else -> false // TrackTable handles its own keys
        }
    }
}

fun playlistStatusBarHint(focus: PlaylistFocus): String = when (focus) {
    PlaylistFocus.Input -> inputFieldStatusBarHint()
    PlaylistFocus.List -> "j/k:Scroll  Enter:Open  a:Add  d:Delete  Tab:Open  Esc:Back"
    PlaylistFocus.TrackList -> "j/k:Scroll  Enter:Play  p:PlayAll  Esc:Back"
}

package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.displayName
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.ui.Text

/** 统一的“把曲目加入本地歌单”选择器状态。 */
@Stable
class PlaylistPickerState {
    var track by mutableStateOf<Track?>(null)
        private set

    val isOpen: Boolean get() = track != null

    internal val selection = ListSelectionState()

    fun open(track: Track) {
        this.track = track
        selection.jumpTo(0)
    }

    fun close() {
        track = null
    }
}

@Composable
fun rememberPlaylistPickerState(): PlaylistPickerState = remember { PlaylistPickerState() }

/** 本地歌单选择器：Search/Queue 共用；Enter 由内部 ListView 处理。 */
@Composable
fun PlaylistPicker(
    state: PlaylistPickerState,
    playlists: List<MediaList>,
    visibleLines: Int,
    onAdd: (playlist: MediaList, track: Track) -> Unit,
) {
    val track = state.track ?: return
    val palette = LocalTuiColors.current

    Text("  Add to local playlist", color = palette.titleFg)
    ListView(
        items = playlists,
        visibleLines = visibleLines,
        emptyMessage = "No local playlists.",
        selection = state.selection,
        onConfirm = { playlist ->
            onAdd(playlist, track)
            state.close()
        },
        render = { playlist, _, selected ->
            Text(
                (if (selected) " ▸ " else "   ") + playlist.displayName(),
                color = if (selected) palette.highlightFg else palette.textDimFg,
            )
        },
    )
}

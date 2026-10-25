package com.eimsound.eimusic.viewmodel

import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.playlist.PlaylistStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

data class PlaylistUiState(
    val playlists: List<MediaList> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val selectedPlaylist: MediaList? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val addUrlInput: String = "",
    val isAddingPlaylist: Boolean = false,
)

/** terminalApp 的薄壳：歌单逻辑在 shared PlaylistStore。 */
class PlaylistViewModel(
    private val store: PlaylistStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(PlaylistUiState())
    val state: StateFlow<PlaylistUiState> = _state.asStateFlow()

    init {
        scope.launch {
            store.state.collect { shared ->
                _state.update {
                    it.copy(
                        playlists = shared.playlists,
                        tracks = shared.tracks,
                        selectedPlaylist = shared.selectedPlaylist,
                        isLoading = shared.isLoading,
                        error = shared.error,
                    )
                }
            }
        }
    }

    fun load() = store.load()
    fun addPlaylist(url: String) = store.addPlaylist(url)
    fun createLocalPlaylist(name: String) = store.createLocalPlaylist(name)
    fun removePlaylist(mediaList: MediaList) = store.removePlaylist(mediaList)
    fun selectPlaylist(mediaList: MediaList) = store.selectPlaylist(mediaList)
    fun refreshPlaylist() = store.refreshPlaylist()
    fun addTrackToPlaylist(playlistId: String, track: Track) = store.addTrackToPlaylist(playlistId, track)
    fun removeTrackAt(index: Int) = store.removeTrackAt(index)
    fun moveTrack(from: Int, to: Int) = store.moveTrack(from, to)

    fun updateAddUrl(url: String) {
        _state.update { it.copy(addUrlInput = url) }
    }

    fun clearError() = store.clearError()

    fun release() {
        store.release()
        scope.cancel()
    }
}

package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.playlist.PlaylistStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyState(
    val tracks: List<Track> = emptyList(),
    val playlists: List<MediaList> = emptyList(),
    val selectedMediaList: MediaList? = null,
    val selectedTrack: Track? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/** composeApp 的薄壳：歌单逻辑在 shared PlaylistStore。 */
class MyViewModel(
    private val store: PlaylistStore,
) : ViewModel() {
    private val _state = MutableStateFlow(MyState())
    val state: StateFlow<MyState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.state.collect { shared ->
                _state.update {
                    it.copy(
                        tracks = shared.tracks,
                        playlists = shared.playlists,
                        selectedMediaList = shared.selectedPlaylist,
                        isLoading = shared.isLoading,
                        error = shared.error,
                    )
                }
            }
        }
    }

    fun load() = store.load()
    fun add(url: String) = store.addPlaylist(url)
    fun remove(mediaList: MediaList) = store.removePlaylist(mediaList)
    fun refreshMediaList(mediaList: MediaList) = store.refreshPlaylist(mediaList)
    fun selectMediaList(mediaList: MediaList) = store.selectPlaylist(mediaList)
    fun clearError() = store.clearError()
}

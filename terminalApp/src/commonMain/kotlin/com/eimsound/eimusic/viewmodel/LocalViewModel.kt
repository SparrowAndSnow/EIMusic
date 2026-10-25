package com.eimsound.eimusic.viewmodel

import com.eimsound.eimusic.local.LocalStore
import com.eimsound.eimusic.music.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocalUiState(
    val tracks: List<Track> = emptyList(),
    val localPaths: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val pathInput: String = "",
)

class LocalViewModel(
    private val store: LocalStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(LocalUiState())
    val state: StateFlow<LocalUiState> = _state.asStateFlow()

    init {
        scope.launch {
            store.state.collect { shared ->
                _state.update {
                    it.copy(
                        tracks = shared.tracks,
                        localPaths = shared.paths,
                        isLoading = shared.isLoading,
                    )
                }
            }
        }
    }

    fun refreshTracks() = store.refresh()

    fun addPath(path: String) {
        store.addPath(path)
        _state.update { it.copy(pathInput = "") }
    }

    fun removePath(path: String) = store.removePath(path)

    fun updatePathInput(input: String) {
        _state.update { it.copy(pathInput = input) }
    }

    fun release() {
        store.release()
        scope.cancel()
    }
}

package com.eimsound.eimusic.viewmodel

import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.util.loadTrackFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocalUiState(
    val tracks: List<Track> = emptyList(),
    val localPaths: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val pathInput: String = "",
)

class LocalViewModel(
    private val storage: IStorage,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(LocalUiState())
    val state: StateFlow<LocalUiState> = _state.asStateFlow()

    init {
        val paths = storage.get(Settings::localPath, emptySet<String>())
        _state.value = _state.value.copy(localPaths = paths)
        loadTracks()
    }

    fun refreshTracks() {
        _state.value = _state.value.copy(isLoading = true)
        scope.launch {
            _state.value = _state.value.copy(
                tracks = loadTrackFiles(_state.value.localPaths),
                isLoading = false,
            )
        }
    }

    fun addPath(path: String) {
        val newPaths = _state.value.localPaths + path
        _state.value = _state.value.copy(localPaths = newPaths, pathInput = "")
        scope.launch {
            storage.save(Settings::localPath, newPaths)
            _state.value = _state.value.copy(
                tracks = loadTrackFiles(newPaths),
            )
        }
    }

    fun removePath(path: String) {
        val newPaths = _state.value.localPaths - path
        _state.value = _state.value.copy(localPaths = newPaths)
        scope.launch {
            storage.save(Settings::localPath, newPaths)
            _state.value = _state.value.copy(
                tracks = loadTrackFiles(newPaths),
            )
        }
    }

    fun updatePathInput(input: String) {
        _state.value = _state.value.copy(pathInput = input)
    }

    private fun loadTracks() {
        _state.value = _state.value.copy(isLoading = true)
        scope.launch {
            _state.value = _state.value.copy(
                tracks = loadTrackFiles(_state.value.localPaths),
                isLoading = false,
            )
        }
    }

    fun release() { scope.cancel() }
}

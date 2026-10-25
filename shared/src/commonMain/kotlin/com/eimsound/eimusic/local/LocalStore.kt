package com.eimsound.eimusic.local

import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocalLibraryState(
    val tracks: List<Track> = emptyList(),
    val paths: Set<String> = emptySet(),
    val isLoading: Boolean = false,
)

/** 本地路径持久化与曲目扫描状态；文件扫描由平台壳注入。 */
class LocalStore(
    private val storage: IStorage,
    private val loadTracks: suspend (Set<String>) -> List<Track>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(
        LocalLibraryState(paths = storage.get(Settings::localPath, emptySet())),
    )
    val state: StateFlow<LocalLibraryState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val paths = _state.value.paths
        _state.update { it.copy(isLoading = true) }
        scope.launch {
            _state.update { it.copy(tracks = loadTracks(paths), isLoading = false) }
        }
    }

    fun addPath(path: String) {
        setPaths(_state.value.paths + path)
    }

    fun removePath(path: String) {
        setPaths(_state.value.paths - path)
    }

    fun setPaths(paths: Set<String>) {
        _state.update { it.copy(paths = paths) }
        scope.launch {
            storage.save(Settings::localPath, paths)
            _state.update { it.copy(tracks = loadTracks(paths)) }
        }
    }

    fun release() = scope.cancel()
}

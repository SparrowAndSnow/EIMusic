package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.data.Storage
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.settings.Settings
import com.eimsound.eimusic.util.loadTrackFiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocalViewModel(val storage: Storage) : ViewModel() {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()
    
    private val _localPaths = MutableStateFlow<Set<String>>(storage.get(Settings::localPath, emptySet()))
    val localPaths: StateFlow<Set<String>> = _localPaths.asStateFlow()

    init {
        loadTracks()
    }

    fun refreshTracks() {
        viewModelScope.launch {
            _tracks.value = loadTrackFiles(_localPaths.value)
        }
    }
    
    fun updateLocalPaths(paths: Set<String>) {
        _localPaths.value = paths
        viewModelScope.launch {
            storage.save(Settings::localPath, paths)
            _tracks.value = loadTrackFiles(paths)
        }
    }
    
    private fun loadTracks() {
        viewModelScope.launch {
            _tracks.value = loadTrackFiles(_localPaths.value)
        }
    }
}
package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import com.eimsound.eimusic.util.loadTrackFiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocalViewModel(private val storage: IStorage) : ViewModel() {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()
    
    private val _localPaths = MutableStateFlow<Set<String>>(storage.get(Settings::localPath, emptySet()))
    val localPaths: StateFlow<Set<String>> = _localPaths.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val  isLoading = _isLoading.asStateFlow()

    init {
        loadTracks()
    }

    fun refreshTracks() {
        _isLoading.value = true
        viewModelScope.launch {
            _tracks.value = loadTrackFiles(_localPaths.value)
            _isLoading.value = false
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
        _isLoading.value = true
        viewModelScope.launch {
            _tracks.value = loadTrackFiles(_localPaths.value)
            _isLoading.value = false
        }
    }
}
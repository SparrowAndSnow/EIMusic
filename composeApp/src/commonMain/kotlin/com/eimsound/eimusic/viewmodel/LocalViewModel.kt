package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.local.LocalStore
import com.eimsound.eimusic.music.Track
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LocalViewModel(
    private val store: LocalStore,
) : ViewModel() {
    val tracks: StateFlow<List<Track>> = store.state
        .map { it.tracks }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val localPaths: StateFlow<Set<String>> = store.state
        .map { it.paths }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val isLoading: StateFlow<Boolean> = store.state
        .map { it.isLoading }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun refreshTracks() = store.refresh()
    fun updateLocalPaths(paths: Set<String>) = store.setPaths(paths)
}

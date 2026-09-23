package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.Result
import com.eimsound.eimusic.factory.TrackRepositoryFactory
import com.eimsound.eimusic.music.TrackSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = false
)

class WelcomeViewModel(
    private val trackRepositoryFactory: TrackRepositoryFactory
) : ViewModel() {

    private val _state = MutableStateFlow(WelcomeUiState())
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    init {
        loadTopTracks()
    }

    fun loadTopTracks() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val trackRepository = trackRepositoryFactory.get(TrackSource.NETEASE)
            when (val result = trackRepository.album("260049502")) {
                is Result.Success -> {
                    result.data?.tracks?.let {
                        _state.value =
                            _state.value.copy(tracks = it, isEmpty = it.isEmpty(), isLoading = false, error = null)
                    } ?: run {
                        _state.value = _state.value.copy(isEmpty = true, isLoading = false, error = null)
                    }
                }

                is Result.Error -> {
                    _state.value = _state.value.copy(error = result.message, isLoading = false)
                }
            }
        }
    }
}


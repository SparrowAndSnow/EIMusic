package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.Result
import com.eimsound.eimusic.db.DbLibraryRepository
import com.eimsound.eimusic.factory.TrackRepositoryFactory
import com.eimsound.eimusic.music.TrackSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class SearchResultState(
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)
class SearchResultViewModel(
    val trackRepositoryFactory: TrackRepositoryFactory,
    private val library: DbLibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchResultState())
    val state: StateFlow<SearchResultState> = _state.asStateFlow()

    fun search(keyword: String) {
        library.recordSearch(keyword)
        _state.value = _state.value.copy(loading = true)
        val trackRepository = trackRepositoryFactory.get(TrackSource.NETEASE)
        viewModelScope.launch {
            when (val result = trackRepository.search(keyword)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(tracks = result.data, loading = false)
                }

                is Result.Error -> {
                    _state.value = _state.value.copy(error = result.message, loading = false)
                }
            }

        }
    }
}
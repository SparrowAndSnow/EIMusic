package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.search.SearchStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchResultState(
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class SearchResultViewModel(
    private val store: SearchStore,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchResultState())
    val state: StateFlow<SearchResultState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.state.collect { shared ->
                _state.update {
                    it.copy(tracks = shared.results, loading = shared.isLoading, error = shared.error)
                }
            }
        }
    }

    fun search(keyword: String) {
        store.updateQuery(keyword)
        store.submitSearch()
    }
}

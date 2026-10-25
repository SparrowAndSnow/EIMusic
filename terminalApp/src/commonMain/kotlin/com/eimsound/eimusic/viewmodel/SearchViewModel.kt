package com.eimsound.eimusic.viewmodel

import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.search.SearchStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val recentSearches: List<String> = emptyList(),
)

class SearchViewModel(
    private val store: SearchStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(SearchUiState(recentSearches = store.state.value.recentSearches))
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        scope.launch {
            store.state.collect { shared ->
                _state.update {
                    it.copy(
                        query = shared.query,
                        results = shared.results,
                        isLoading = shared.isLoading,
                        error = shared.error,
                        recentSearches = shared.recentSearches,
                    )
                }
            }
        }
    }

    fun updateQuery(query: String) = store.updateQuery(query)
    fun submitSearch() = store.submitSearch()
    fun reset() = store.reset()

    fun release() {
        store.release()
        scope.cancel()
    }
}

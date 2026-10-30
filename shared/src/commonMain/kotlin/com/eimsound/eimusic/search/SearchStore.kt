package com.eimsound.eimusic.search

import com.eimsound.eimusic.Result
import com.eimsound.eimusic.db.DbLibraryRepository
import com.eimsound.eimusic.factory.TrackRepositoryFactory
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val results: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val recentSearches: List<String> = emptyList(),
)

/** 与壳无关的搜索逻辑与历史；terminal/compose 共用。 */
class SearchStore(
    private val trackRepositoryFactory: TrackRepositoryFactory,
    private val library: DbLibraryRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(SearchState(recentSearches = library.recentSearches()))
    val state: StateFlow<SearchState> = _state.asStateFlow()

    fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun submitSearch() {
        val query = _state.value.query
        if (query.isBlank()) return
        recordRecentSearch(query)
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            runCatching {
                val repo = trackRepositoryFactory.get(TrackSource.NETEASE)
                when (val result = repo.search(query)) {
                    is Result.Success -> _state.update { it.copy(results = result.data, isLoading = false) }
                    is Result.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
                }
            }.onFailure { error ->
                _state.update { it.copy(error = "Exception: ${error.message}", isLoading = false) }
            }
        }
    }

    fun reset() {
        _state.update { SearchState(recentSearches = it.recentSearches) }
    }

    fun release() = scope.cancel()

    private fun recordRecentSearch(query: String) {
        val recent = (listOf(query) + _state.value.recentSearches.filterNot {
            it.equals(query, ignoreCase = true)
        }).take(8)
        _state.update { it.copy(recentSearches = recent) }
        runCatching { library.recordSearch(query) }
    }
}

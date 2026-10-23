package com.eimsound.eimusic.viewmodel

import com.eimsound.eimusic.Result
import com.eimsound.eimusic.factory.TrackRepositoryFactory
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.db.DbLibraryRepository
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SearchUiState(
    val query: String = "",
    val results: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val recentSearches: List<String> = emptyList(),
)

class SearchViewModel(
    private val trackRepositoryFactory: TrackRepositoryFactory,
    private val storage: IStorage,
    private val library: DbLibraryRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(
        SearchUiState(recentSearches = library.recentSearches()),
    )
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init { migrateLegacySearches() }

    fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun submitSearch() {
        val query = _state.value.query
        if (query.isBlank()) return
        recordRecentSearch(query)
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            try {
                val repo = trackRepositoryFactory.get(TrackSource.NETEASE)
                when (val result = repo.search(query)) {
                    is Result.Success -> _state.update { it.copy(results = result.data, isLoading = false) }
                    is Result.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Exception: ${e.message}", isLoading = false) }
            }
        }
    }

    private fun recordRecentSearch(query: String) {
        val recent = (listOf(query) + _state.value.recentSearches.filterNot { it.equals(query, ignoreCase = true) })
            .take(8)
        _state.update { it.copy(recentSearches = recent) }
        runCatching { library.recordSearch(query) }
    }

    fun reset() {
        _state.update { SearchUiState(recentSearches = _state.value.recentSearches) }
    }

    private fun migrateLegacySearches() {
        if (_state.value.recentSearches.isNotEmpty()) return
        val legacy = runCatching { storage.get(Settings::recentSearches, emptyList<String>()) }.getOrDefault(emptyList())
        if (legacy.isEmpty()) return
        runCatching {
            legacy.forEach(library::recordSearch)
            _state.update { it.copy(recentSearches = library.recentSearches()) }
        }
    }

    fun release() { scope.cancel() }
}

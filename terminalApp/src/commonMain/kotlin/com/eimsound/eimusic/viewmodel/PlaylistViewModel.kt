package com.eimsound.eimusic.viewmodel

import com.eimsound.eimusic.Result
import com.eimsound.eimusic.factory.ShareLinkResolverFactory
import com.eimsound.eimusic.factory.TrackRepositoryFactory
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.MediaListId
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.resolver.share.toMediaListId
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaylistUiState(
    val playlists: List<MediaList> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val selectedPlaylist: MediaList? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val addUrlInput: String = "",
    val isAddingPlaylist: Boolean = false,
)

class PlaylistViewModel(
    private val trackRepositoryFactory: TrackRepositoryFactory,
    private val shareLinkResolverFactory: ShareLinkResolverFactory,
    private val storage: IStorage,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(PlaylistUiState())
    val state: StateFlow<PlaylistUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        scope.launch {
            val playlistsStorage = storage.get(Settings::playlists, emptyMap<String, String>())
            _state.value = _state.value.copy(isLoading = true)

            val resultDeferred = playlistsStorage.map {
                async {
                    val mediaListId = MediaListId.parse(it.key)
                    val trackRepository = trackRepositoryFactory.get(mediaListId.source)
                    trackRepository.universalMediaList(mediaListId.type, mediaListId.id)
                }
            }
            val playlists = resultDeferred.awaitAll().map {
                when (it) {
                    is Result.Success -> it.data
                    else -> null
                }
            }
            _state.value = _state.value.copy(
                playlists = playlists.filterNotNull(),
                isLoading = false,
            )
        }
    }

    fun addPlaylist(url: String) {
        scope.launch {
            val result = runCatching {
                fetchMediaList(url) ?: return@launch
            }.getOrElse { return@launch }
            _state.value = _state.value.copy(playlists = _state.value.playlists + result)
            store()
        }
    }

    private suspend fun fetchMediaList(url: String): MediaList? {
        val shareLinkResolver = shareLinkResolverFactory.get(url)
        val shareLinkType = shareLinkResolver.resolve(url)
        val mediaListId = shareLinkType.toMediaListId() ?: return null
        return fetchMediaList(mediaListId)
    }

    private suspend fun fetchMediaList(id: MediaListId): MediaList? {
        _state.value = _state.value.copy(isLoading = true)
        val trackRepository = trackRepositoryFactory.get(id.source)
        when (val result = trackRepository.universalMediaList(id.type, id.id)) {
            is Result.Success -> {
                val data = result.data
                if (_state.value.playlists.any { it.id == data.id }) return null
                _state.value = _state.value.copy(isLoading = false)
                return data
            }
            is Result.Error -> {
                _state.value = _state.value.copy(error = result.message, isLoading = false)
                return null
            }
        }
    }

    fun removePlaylist(mediaList: MediaList) {
        val updatedPlaylists = _state.value.playlists.filter { it != mediaList }
        _state.value = _state.value.copy(playlists = updatedPlaylists)
        store()
    }

    fun selectPlaylist(mediaList: MediaList) {
        scope.launch {
            val ml = _state.value.playlists.find { it.id == mediaList.id } ?: return@launch
            val tracks = when (ml) {
                is MediaList.PlaylistMediaList -> ml.playlist.tracks
                is MediaList.AlbumMediaList -> ml.album.tracks
            }
            if (tracks.isEmpty()) {
                val result = fetchMediaList(ml.id) ?: return@launch
                val resultTracks = when (result) {
                    is MediaList.PlaylistMediaList -> result.playlist.tracks
                    is MediaList.AlbumMediaList -> result.album.tracks
                }
                _state.value = _state.value.copy(
                    selectedPlaylist = result,
                    tracks = resultTracks,
                )
            } else {
                _state.value = _state.value.copy(
                    selectedPlaylist = ml,
                    tracks = tracks,
                )
            }
        }
    }

    fun refreshPlaylist() {
        val selected = _state.value.selectedPlaylist ?: return
        scope.launch {
            val result = fetchMediaList(selected.id) ?: return@launch
            val tracks = when (result) {
                is MediaList.PlaylistMediaList -> result.playlist.tracks
                is MediaList.AlbumMediaList -> result.album.tracks
            }
            _state.value = _state.value.copy(tracks = tracks)
        }
    }

    fun updateAddUrl(url: String) {
        _state.value = _state.value.copy(addUrlInput = url)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun store() {
        storage.save(
            Settings::playlists,
            _state.value.playlists.associate {
                val name = when (it) {
                    is MediaList.PlaylistMediaList -> it.playlist.name
                    is MediaList.AlbumMediaList -> it.album.name
                }
                it.id.toString() to name
            }
        )
    }

    fun release() { scope.cancel() }
}

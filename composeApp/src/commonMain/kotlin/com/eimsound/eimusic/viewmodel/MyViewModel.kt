package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.Result
import com.eimsound.eimusic.factory.ShareLinkResolverFactory
import com.eimsound.eimusic.factory.TrackRepositoryFactory
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.MediaListId
import com.eimsound.eimusic.resolver.share.toMediaListId
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.emptyMap

data class MyState(
    val tracks: List<Track> = emptyList(),
    val playlists: List<MediaList> = emptyList(),
    val selectedMediaList: MediaList? = null,
    val selectedTrack: Track? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)


class MyViewModel(
    private val trackRepositoryFactory: TrackRepositoryFactory,
    private val shareLinkResolverFactory: ShareLinkResolverFactory,
    private val storage: IStorage,
) : ViewModel() {
    private val _state = MutableStateFlow(MyState())
    val state: StateFlow<MyState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val playlistsStorage = storage.get(Settings::playlists, emptyMap<String, String>())

            _state.value = _state.value.copy(
                isLoading = true,
            )
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

    fun add(url: String) {
        viewModelScope.launch {
            val result = fetchMediaList(url)
            result ?: return@launch
            _state.value = _state.value.copy(
                playlists = _state.value.playlists + result,
            )
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
        _state.value = _state.value.copy(
            isLoading = true,
        )
        val trackRepository = trackRepositoryFactory.get(id.source)
        when (val result = trackRepository.universalMediaList(id.type, id.id)) {
            is Result.Success -> {
                val data = result.data
                if (_state.value.playlists.any { it.id == data.id }) {
                    return null
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                )
                return data
            }

            is Result.Error -> {
                _state.value = _state.value.copy(
                    error = result.message,
                    isLoading = false,
                )
                return null
            }
        }
    }

    fun remove(mediaList: MediaList) {
        val updatedPlaylists = _state.value.playlists.filter { it != mediaList }
        val removingSelected = _state.value.selectedMediaList?.id == mediaList.id
        val fallbackSelected = if (removingSelected) updatedPlaylists.firstOrNull() else _state.value.selectedMediaList

        val tracks = when (fallbackSelected) {
            is MediaList.PlaylistMediaList -> fallbackSelected.playlist.tracks
            is MediaList.AlbumMediaList -> fallbackSelected.album.tracks
            else -> null
        }

        _state.value = _state.value.copy(
            playlists = updatedPlaylists,
            selectedMediaList = fallbackSelected,
            selectedTrack = if (removingSelected) null else _state.value.selectedTrack,
            tracks = tracks ?: if (removingSelected) emptyList() else _state.value.tracks,
        )
        store()
    }

    fun refreshMediaList(mediaList: MediaList) {
        viewModelScope.launch {
            val result = fetchMediaList(mediaList.id)
            result ?: return@launch

            val tracks = when (result) {
                is MediaList.PlaylistMediaList -> result.playlist.tracks
                is MediaList.AlbumMediaList -> result.album.tracks
            }
            _state.value = _state.value.copy(
                tracks = tracks,
            )
        }
    }

    fun selectMediaList(selectedMediaList: MediaList) {
        viewModelScope.launch {
            val mediaList = _state.value.playlists.find { it.id == selectedMediaList.id }
            mediaList ?: return@launch
            val mediaListTrack = when (mediaList) {
                is MediaList.PlaylistMediaList -> mediaList.playlist.tracks
                is MediaList.AlbumMediaList -> mediaList.album.tracks
            }
            if (mediaListTrack.isEmpty()) {
                val result = fetchMediaList(mediaList.id)
                result ?: return@launch
                val tracks = when (result) {
                    is MediaList.PlaylistMediaList -> result.playlist.tracks
                    is MediaList.AlbumMediaList -> result.album.tracks
                }
                _state.value = _state.value.copy(
                    playlists = _state.value.playlists + result,
                    selectedMediaList = result,
                    tracks = tracks,
                )
            } else {
                _state.value = _state.value.copy(
                    selectedMediaList = mediaList,
                    tracks = mediaListTrack,
                )
            }

        }
    }

    fun clearError() {
        _state.value = _state.value.copy(
            error = null,
        )
    }

    private fun store() {
        storage.save(
            Settings::playlists,
            _state.value.playlists
                .associate {
                    val name = when (it) {
                        is MediaList.PlaylistMediaList -> {
                            it.playlist.name
                        }

                        is MediaList.AlbumMediaList -> {
                            it.album.name
                        }
                    }
                    it.id.toString() to name
                }
        )
    }
}

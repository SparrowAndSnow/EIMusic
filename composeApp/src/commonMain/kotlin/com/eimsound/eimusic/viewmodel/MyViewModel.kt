package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.Result
import com.eimsound.eimusic.db.DbPlaylist
import com.eimsound.eimusic.db.DbPlaylistRepository
import com.eimsound.eimusic.db.DbTrack
import com.eimsound.eimusic.db.toDbTrack
import com.eimsound.eimusic.db.toTrack
import com.eimsound.eimusic.factory.ShareLinkResolverFactory
import com.eimsound.eimusic.factory.TrackRepositoryFactory
import com.eimsound.eimusic.music.MediaList
import com.eimsound.eimusic.music.MediaListId
import com.eimsound.eimusic.music.MediaListType
import com.eimsound.eimusic.music.Playlist
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.resolver.share.toMediaListId
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val db: DbPlaylistRepository,
    private val storage: IStorage,
) : ViewModel() {
    private val _state = MutableStateFlow(MyState())
    val state: StateFlow<MyState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            migrateLegacyPlaylists()
            _state.update { it.copy(isLoading = true) }
            val playlists = (db.localPlaylists() + db.importedPlaylists()).map(DbPlaylist::toMediaList)
            _state.update {
                it.copy(
                    playlists = playlists,
                    selectedMediaList = it.selectedMediaList?.let { selected ->
                        playlists.find { p -> p.id == selected.id }
                    },
                    tracks = if (it.selectedMediaList == null) emptyList() else it.tracks,
                    isLoading = false,
                )
            }
        }
    }

    fun add(url: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val mediaList = fetchMediaList(url)
            if (mediaList == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            importMediaList(mediaList, mediaList.displayName())
            load()
        }
    }

    private suspend fun fetchMediaList(url: String): MediaList? {
        val shareLinkResolver = shareLinkResolverFactory.get(url)
        val shareLinkType = shareLinkResolver.resolve(url)
        val mediaListId = shareLinkType.toMediaListId() ?: return null
        return fetchMediaList(mediaListId)
    }

    private suspend fun fetchMediaList(id: MediaListId): MediaList? {
        _state.update { it.copy(isLoading = true) }
        val trackRepository = trackRepositoryFactory.get(id.source)
        return when (val result = trackRepository.universalMediaList(id.type, id.id)) {
            is Result.Success -> {
                _state.update { it.copy(isLoading = false) }
                result.data
            }

            is Result.Error -> {
                _state.update { it.copy(error = result.message, isLoading = false) }
                null
            }
        }
    }

    fun remove(mediaList: MediaList) {
        viewModelScope.launch {
            db.deletePlaylistRecord(mediaList.id.id)
            val removingSelected = _state.value.selectedMediaList?.id == mediaList.id
            _state.update {
                it.copy(
                    selectedMediaList = if (removingSelected) null else it.selectedMediaList,
                    selectedTrack = if (removingSelected) null else it.selectedTrack,
                    tracks = if (removingSelected) emptyList() else it.tracks,
                )
            }
            load()
        }
    }

    fun refreshMediaList(mediaList: MediaList) {
        if (mediaList.id.source == TrackSource.LOCAL) return
        viewModelScope.launch {
            val fetched = fetchMediaList(mediaList.id) ?: return@launch
            importMediaList(fetched, fetched.displayName())
            val stored = db.playlist(mediaList.id.id) ?: return@launch
            _state.update {
                it.copy(
                    selectedMediaList = stored.toMediaList(),
                    tracks = stored.tracks.map(DbTrack::toTrack),
                )
            }
            load()
        }
    }

    fun selectMediaList(selectedMediaList: MediaList) {
        viewModelScope.launch {
            val stored = db.playlist(selectedMediaList.id.id) ?: run {
                val fetched = fetchMediaList(selectedMediaList.id) ?: return@launch
                importMediaList(fetched, fetched.displayName())
                db.playlist(selectedMediaList.id.id) ?: return@launch
            }
            _state.update {
                it.copy(
                    selectedMediaList = stored.toMediaList(),
                    tracks = stored.tracks.map(DbTrack::toTrack),
                )
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun migrateLegacyPlaylists() {
        val saved = storage.get(Settings::playlists, emptyMap<String, String>())
        if (saved.isEmpty()) return
        var migrated = true
        saved.forEach { (key, name) ->
            runCatching {
                val id = MediaListId.parse(key)
                val mediaList = fetchMediaList(id) ?: return@forEach
                importMediaList(mediaList, name)
            }.onFailure { migrated = false }
        }
        if (migrated) {
            storage.save(Settings::playlists, emptyMap<String, String>())
        }
    }

    private fun importMediaList(mediaList: MediaList, name: String) {
        val id = mediaList.id
        if (id.source == TrackSource.LOCAL) return
        db.importPlaylist(
            source = id.source,
            externalId = id.id,
            name = name,
            importedTracks = mediaList.allTracks().map(Track::toDbTrack),
        )
    }
}

private fun DbPlaylist.toMediaList(): MediaList.PlaylistMediaList {
    val mediaListId = MediaListId(source, MediaListType.PLAYLIST, id)
    val domainTracks = tracks.map(DbTrack::toTrack)
    return MediaList.PlaylistMediaList(
        playlist = Playlist(
            tracks = domainTracks,
            name = name,
            id = id,
            creator = null,
            image = null,
            description = null,
            totalTracks = domainTracks.size,
        ),
        source = source,
        id = mediaListId,
    )
}

private fun MediaList.allTracks(): List<Track> = when (this) {
    is MediaList.PlaylistMediaList -> playlist.tracks
    is MediaList.AlbumMediaList -> album.tracks
}

private fun MediaList.displayName(): String = when (this) {
    is MediaList.PlaylistMediaList -> playlist.name
    is MediaList.AlbumMediaList -> album.name
}

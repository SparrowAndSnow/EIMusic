package com.eimsound.eimusic.playlist

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
import com.eimsound.eimusic.music.displayName
import com.eimsound.eimusic.resolver.share.toMediaListId
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Settings
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.emptyMap

data class PlaylistState(
    val playlists: List<MediaList> = emptyList(),
    val selectedPlaylist: MediaList? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

/** 与壳无关的歌单/导入/选中逻辑；terminal 和 compose 共用。 */
class PlaylistStore(
    private val trackRepositoryFactory: TrackRepositoryFactory,
    private val shareLinkResolverFactory: ShareLinkResolverFactory,
    private val storage: IStorage,
    private val db: DbPlaylistRepository,
    private val log: (String) -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(PlaylistState())
    val state: StateFlow<PlaylistState> = _state.asStateFlow()

    init {
        scope.launch {
            migrateLegacyPlaylists()
            reloadFromDb()
        }
    }

    fun load() {
        scope.launch { reloadFromDb() }
    }

    fun addPlaylist(url: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = runCatching { fetchMediaList(url) }
            val mediaList = result.getOrNull()
            if (mediaList == null) {
                _state.update { it.copy(isLoading = false) }
                reportError(result.exceptionOrNull() ?: IllegalArgumentException("Unsupported playlist URL"))
                return@launch
            }
            runCatching { importMediaList(mediaList, mediaList.displayName()) }
                .onSuccess { reloadFromDb() }
                .onFailure(::reportError)
        }
    }

    fun createLocalPlaylist(name: String) {
        scope.launch {
            if (name.isBlank()) return@launch
            runCatching { db.createLocal(name.trim()) }
                .onSuccess { reloadFromDb() }
                .onFailure(::reportError)
        }
    }

    fun removePlaylist(mediaList: MediaList) {
        scope.launch {
            db.deletePlaylistRecord(mediaList.id.id)
            if (_state.value.selectedPlaylist?.id == mediaList.id) {
                _state.update { it.copy(selectedPlaylist = null, tracks = emptyList()) }
            }
            reloadFromDb()
        }
    }

    fun selectPlaylist(mediaList: MediaList) {
        scope.launch {
            val stored = db.playlist(mediaList.id.id) ?: run {
                val fetched = fetchMediaList(mediaList.id) ?: return@launch
                importMediaList(fetched, fetched.displayName())
                db.playlist(mediaList.id.id) ?: return@launch
            }
            _state.update {
                it.copy(
                    selectedPlaylist = stored.toMediaList(),
                    tracks = stored.tracks.map(DbTrack::toTrack),
                )
            }
        }
    }

    fun refreshPlaylist(mediaList: MediaList? = _state.value.selectedPlaylist) {
        if (mediaList == null || mediaList.id.source == TrackSource.LOCAL) return
        scope.launch {
            val fetched = fetchMediaList(mediaList.id) ?: return@launch
            importMediaList(fetched, fetched.displayName())
            val stored = db.playlist(mediaList.id.id) ?: return@launch
            _state.update {
                it.copy(
                    selectedPlaylist = stored.toMediaList(),
                    tracks = stored.tracks.map(DbTrack::toTrack),
                )
            }
            reloadFromDb(preserveSelection = true)
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        scope.launch {
            runCatching { db.addTrack(playlistId, track.toDbTrack()) }
                .onSuccess { if (_state.value.selectedPlaylist?.id?.id == playlistId) reloadSelectedTracks() }
                .onFailure(::reportError)
        }
    }

    fun removeTrackAt(index: Int) {
        val selected = _state.value.selectedPlaylist ?: return
        val track = _state.value.tracks.getOrNull(index) ?: return
        scope.launch {
            db.removeTrack(selected.id.id, track.id.toString())
            reloadSelectedTracks()
        }
    }

    fun moveTrack(from: Int, to: Int) {
        val selected = _state.value.selectedPlaylist ?: return
        scope.launch {
            db.moveTrack(selected.id.id, from, to)
            reloadSelectedTracks()
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun release() = scope.cancel()

    private suspend fun reloadFromDb(preserveSelection: Boolean = false) {
        _state.update { it.copy(isLoading = true) }
        val playlists = (db.localPlaylists() + db.importedPlaylists()).map(DbPlaylist::toMediaList)
        val selectedId = if (preserveSelection) _state.value.selectedPlaylist?.id else null
        val selected = selectedId?.let { id -> playlists.find { it.id == id } }
        _state.update {
            it.copy(
                playlists = playlists,
                selectedPlaylist = if (preserveSelection) selected else it.selectedPlaylist,
                tracks = if (preserveSelection) it.tracks else it.tracks,
                isLoading = false,
            )
        }
    }

    private fun reloadSelectedTracks() {
        val selected = _state.value.selectedPlaylist ?: return
        val stored = db.playlist(selected.id.id) ?: return
        _state.update {
            it.copy(
                selectedPlaylist = stored.toMediaList(),
                tracks = stored.tracks.map(DbTrack::toTrack),
            )
        }
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
        if (migrated) storage.save(Settings::playlists, emptyMap<String, String>())
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

    private suspend fun fetchMediaList(url: String): MediaList? {
        val shareLinkResolver = shareLinkResolverFactory.get(url)
        val shareLinkType = shareLinkResolver.resolve(url)
        val mediaListId = shareLinkType.toMediaListId() ?: return null
        return fetchMediaList(mediaListId)
    }

    private suspend fun fetchMediaList(id: MediaListId): MediaList? {
        _state.update { it.copy(isLoading = true) }
        return when (val result = trackRepositoryFactory.get(id.source).universalMediaList(id.type, id.id)) {
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

    private fun reportError(error: Throwable) {
        log("Playlist error: ${error::class.simpleName}: ${error.message}")
        _state.update { it.copy(error = error.message ?: error::class.simpleName ?: "unknown error", isLoading = false) }
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

package com.eimsound.eimusic.media

import android.os.Bundle
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaMetadata
import org.koin.core.component.KoinComponent
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.session.SessionCommand
import com.eimsound.eimusic.EIMusicMediaController
import com.eimsound.eimusic.cycle
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackId
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.playback.PlaybackOrigin
import com.eimsound.eimusic.playback.toEventSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/**
 * Android 平台的媒体播放器控制器实现
 */
actual class MediaPlayerController actual constructor(val trackUriResolverFactory: TrackUriResolverFactory) :
    IMediaPlayerController,
    KoinComponent {
    private val eimMediaController: EIMusicMediaController by inject()
    private var progressUpdateHandler = Handler(Looper.getMainLooper())
    private val playerListener = createPlayerListener()
    private val mediaController get() = eimMediaController.mediaController
    private var pendingSeekCallback: (() -> Unit)? = null
    private var currentOrigin = PlaybackOrigin.LocalUi
    private var isSeeking = MutableStateFlow(false)
    private var pendingReadyCallback: (() -> Unit)? = null
    private var isReady = MutableStateFlow(false)
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _state = MutableStateFlow(PlayerState())
    actual override val state: StateFlow<PlayerState> = _state.asStateFlow()
    private val _event = MutableSharedFlow<PlaybackEvent>(replay = 0)
    actual override val event: SharedFlow<PlaybackEvent> = _event.asSharedFlow()

    init {
        eimMediaController.isConnected.onEach {
            if (it) {
                mediaController?.addListener(playerListener)
            }
        }.launchIn(coroutineScope)
    }

    private fun createPlayerListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> handlePlayerReady()
                Player.STATE_BUFFERING -> _state.update { it.copy(isLoading = true) }
                Player.STATE_ENDED -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateProgressUpdate(isPlaying)
            _state.update {
                it.copy(playState = if (isPlaying) PlayState.PLAYING else PlayState.PAUSED)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            stopProgressUpdate()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (reason in listOf(
                    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
                    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY
                )
            ) {
                coroutineScope.launch {
                    _event.emit(PlaybackEvent.PlayStateChanged(playWhenReady, source = EventSource.System))
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            mediaItem?.mediaId?.let { id ->
                handleMediaItemTransition(id, reason)
            }
        }
    }

    private fun handlePlayerReady() {
        _state.update {
            it.copy(
                duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L,
                isLoading = false
            )
        }

        if (isReady.value) {
            isReady.value = false
            pendingReadyCallback?.invoke()
            pendingReadyCallback = null
        }

        if (isSeeking.value) {
            isSeeking.value = false
            pendingSeekCallback?.invoke()
            pendingSeekCallback = null
        }

        startProgressUpdate()
    }

    private fun handleMediaItemTransition(mediaId: String, reason: Int) {
        val list = getCurrentPlayList()
        val index = list.indexOfFirst { track -> TrackId.parse(mediaId) == track.id }
        val current = list.getOrNull(index) ?: return

        _state.update {
            it.copy(
                current = current,
                duration = mediaController?.duration ?: 0,
                index = index,
                isLoading = false
            )
        }

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            coroutineScope.launch {
                _event.emit(
                    PlaybackEvent.TrackChanged(
                        current,
                        isAudioCompleted = true,
                        source = if (currentOrigin == PlaybackOrigin.RemoteSync) EventSource.Remote else EventSource.Player
                    )
                )
            }
        }
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaController?.takeIf { it.isPlaying }?.let { controller ->
                val buffered = if (controller.duration > 0)
                    controller.bufferedPosition.toFloat() / controller.duration
                else 0f

                _state.update { state ->
                    state.copy(
                        bufferProgress = buffered.coerceIn(0f, 1f),
                        position = controller.currentPosition,
                    )
                }
            }
            progressUpdateHandler.postDelayed(this, 500)
        }
    }

    private fun updateProgressUpdate(isPlaying: Boolean) {
        if (isPlaying) startProgressUpdate() else stopProgressUpdate()
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        progressUpdateHandler.post(progressRunnable)
    }

    private fun stopProgressUpdate() {
        progressUpdateHandler.removeCallbacks(progressRunnable)
    }


    actual override fun release() {
        stopProgressUpdate()
    }

    actual override fun seek(millis: Long, seekOver: () -> Unit, origin: PlaybackOrigin) {
        if (!isSeeking.value) isSeeking.value = true
        pendingSeekCallback = seekOver
        mediaController?.seekTo(millis)
    }

    actual override fun play(media: Track, origin: PlaybackOrigin) {
        play(media, 0, origin)
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else progressUpdateHandler.post(block)
    }

    private fun play(media: Track, position: Long, origin: PlaybackOrigin = PlaybackOrigin.LocalUi) {
        currentOrigin = origin
        runOnMain {
            playOnMain(media, position, origin)
        }
    }

    private fun playOnMain(media: Track, position: Long, origin: PlaybackOrigin) {
        val list = getCurrentPlayList()
        val mediaIndex = list.indexOfFirst { it.id == media.id }
        // 会话恢复只恢复了状态，ExoPlayer 还没有 media items；首次播放时补上。
        if ((mediaController?.mediaItemCount ?: 0) == 0 && list.isNotEmpty()) {
            mediaController?.setMediaItems(list.mapNotNull { it.parse() })
            mediaController?.prepare()
        }
        _state.update {
            it.copy(
                current = media,
                index = mediaIndex,
                isLoading = true
            )
        }
        mediaController?.seekTo(mediaIndex, position)
        mediaController?.play()
    }

    private fun getCurrentPlayList(): List<Track> = _state.value.currentList

    actual override fun queue(medias: List<Track>, prepareOver: () -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            progressUpdateHandler.post { queue(medias, prepareOver) }
            return
        }
        val mediaItems = medias.mapNotNull { it.parse() }
        _state.update {
            it.copy(queue = medias)
        }
        mediaController?.setMediaItems(mediaItems)
        mediaController?.prepare()
        mediaController?.playWhenReady = true
        pendingReadyCallback = prepareOver
        isReady.value = true
    }

    override fun restore(queue: List<Track>, currentId: TrackId?) {
        val index = queue.indexOfFirst { it.id == currentId }
        val current = queue.getOrNull(index)
        _state.update {
            it.copy(
                queue = queue,
                index = index.coerceAtLeast(0),
                current = current,
                playState = if (current == null) PlayState.STOPPED else PlayState.PAUSED,
                position = 0L,
                isLoading = false,
            )
        }
    }

    actual override fun playState(state: PlayState, origin: PlaybackOrigin) {
        currentOrigin = origin
        if (Looper.myLooper() != Looper.getMainLooper()) {
            progressUpdateHandler.post { playState(state, origin) }
            return
        }
        if (state == PlayState.PLAYING && (mediaController?.mediaItemCount ?: 0) == 0) {
            val list = _state.value.currentList
            if (list.isNotEmpty()) {
                mediaController?.setMediaItems(list.mapNotNull { it.parse() })
                mediaController?.seekTo(_state.value.index.coerceAtLeast(0), _state.value.position)
                mediaController?.prepare()
            }
        }
        when (state) {
            PlayState.PLAYING -> mediaController?.play()
            PlayState.PAUSED -> mediaController?.pause()
            PlayState.STOPPED -> mediaController?.stop()
        }
    }

    actual override fun volume(volume: Float, isMuted: Boolean) {
        mediaController?.volume = if (isMuted) 0f else volume
        _state.update {
            it.copy(volume = volume, isMute = isMuted)
        }
    }

    actual override fun sync(syncState: SyncState) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            progressUpdateHandler.post { sync(syncState) }
            return
        }
        val medias = syncState.trackList ?: return
        val index = medias.indexOfFirst { it.id == syncState.track?.id }

        _state.update {
            it.copy(
                queue = medias,
                index = index,
                current = syncState.track ?: it.current,
                playMode = syncState.playMode ?: it.playMode,
                playState = syncState.playState ?: it.playState,
                position = syncState.position ?: it.position,
            )
        }

        currentOrigin = PlaybackOrigin.RemoteSync
        queue(medias) {
            playMode(_state.value.playMode, PlaybackOrigin.RemoteSync)
            syncState.track?.let { track ->
                play(track, syncState.position ?: 0, PlaybackOrigin.RemoteSync)
                mediaController?.playWhenReady = true
            }
        }
    }


    actual override fun playMode(mode: PlayMode, origin: PlaybackOrigin) {
        when (mode) {
            is PlayMode.Shuffle -> {
                shuffle(mode.seed, mode.indices)
                mediaController?.repeatMode = Player.REPEAT_MODE_ALL
            }

            is PlayMode.Loop -> {
                mediaController?.shuffleModeEnabled = false
                mediaController?.repeatMode = Player.REPEAT_MODE_ALL
            }

            is PlayMode.RepeatOne -> {
                mediaController?.shuffleModeEnabled = false
                mediaController?.repeatMode = Player.REPEAT_MODE_ONE
            }
        }
        _state.update {
            it.copy(playMode = mode)
        }
    }

    private fun Track.parse(): MediaItem? {
        val build = MediaItem.Builder()
            .setMediaId(this.id.toString())
            .setUri(this.id.toString())
            .setTag(this)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(this.name)
                    .setArtist(this.artists?.joinToString(", ") { it.name ?: "" })
                    .setAlbumTitle(this.album?.name)
                    .setArtworkUri(this.album?.image?.toUri())
                    .build()
            )
            .build()

        return build
    }

    private fun shuffle(randomSeed: Long, indices: IntArray) {
        val args = Bundle().apply {
            putLong("seed", randomSeed)
            putIntArray("indices", indices)
        }
        val command = SessionCommand("ACTION_SET_SHUFFLE_SEED", Bundle.EMPTY)
        mediaController?.sendCustomCommand(command, args)
        mediaController?.shuffleModeEnabled = true
    }

    actual override fun next(isAudioCompleted: Boolean, origin: PlaybackOrigin) {
        currentOrigin = origin
        mediaController?.seekToNextMediaItem()
    }

    actual override fun previous(origin: PlaybackOrigin) {
        currentOrigin = origin
        mediaController?.seekToPreviousMediaItem()
    }

    actual override fun nextTrack(): Track? {
        val list = getCurrentPlayList()
        if (list.isEmpty()) return null
        val nextIndex = mediaController?.nextMediaItemIndex ?: 0
        val index = nextIndex.cycle(0, list.size - 1)
        return getTrackFromMediaItem(index)
    }

    actual override fun previousTrack(): Track? {
        val list = getCurrentPlayList()
        if (list.isEmpty()) return null
        val previousIndex = mediaController?.previousMediaItemIndex ?: 0
        val index = previousIndex.cycle(0, list.size - 1)
        return getTrackFromMediaItem(index)
    }

    private fun getTrackFromMediaItem(index: Int): Track? {
        val mediaItem = mediaController?.getMediaItemAt(index)
        val track = mediaItem?.localConfiguration?.tag as? Track
        return track ?: state.value.queue.getOrNull(index)
    }

    actual override fun addTrack(media: Track, index: Int) {
        val mediaItem = media.parse()
        if (mediaItem != null) {
            val existingIndex = state.value.queue.indexOfFirst { track -> track.id == media.id }
            _state.update {
                val updatedQueue = it.queue.toMutableList().apply {
                    if (existingIndex != -1) {
                        mediaController?.removeMediaItem(existingIndex)
                        removeAt(existingIndex)
                    }
                    add(index, media)
                    mediaController?.addMediaItem(index, mediaItem)
                }

                it.copy(
                    queue = updatedQueue,
                    index = it.index.cycle(0, updatedQueue.size - 1)
                )
            }
            mediaController?.prepare()
        }
    }

    actual override fun removeTrack(index: Int) {
        _state.update {
            val updatedQueue = it.queue.filterIndexed { i, _ -> i != index }
            it.copy(
                queue = updatedQueue,
                index = if (index < it.index) it.index - 1 else it.index
            )
        }
        mediaController?.removeMediaItem(index)
        mediaController?.prepare()
    }
}
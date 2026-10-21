package com.eimsound.eimusic.media

import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.nextTrackCandidate
import com.eimsound.eimusic.music.previousTrackCandidate
import com.eimsound.eimusic.music.withTrackAdded
import com.eimsound.eimusic.music.withTrackRemoved
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.goxr3plus.streamplayer.enums.Status
import com.goxr3plus.streamplayer.stream.StreamPlayer
import com.goxr3plus.streamplayer.stream.StreamPlayerEvent
import com.goxr3plus.streamplayer.stream.StreamPlayerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URI

open class JavaStreamPlayerController(
    val trackUriResolverFactory: TrackUriResolverFactory
) : IMediaPlayerController {



    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<PlaybackEvent>(replay = 0)
    override val event: SharedFlow<PlaybackEvent> = _event.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var pendingReadyCallback: (() -> Unit)? = null
    private var isReady = MutableStateFlow(false)

    private val listener = object : StreamPlayerListener {
        override fun opened(p0: Any?, p1: Map<String?, Any?>?) {
        }

        override fun progress(
            nEncodedBytes: Int,
            millisecondsPosition: Long,
            pcmData: ByteArray?,
            properties: Map<String?, Any?>?
        ) {
            _state.update {
                it.copy(position = millisecondsPosition , bufferProgress = mediaPlayer.bufferedPercentage / 100.0F)
            }
        }

        override fun statusUpdated(p0: StreamPlayerEvent) {
            val status: Status = p0.playerStatus
            when (status) {
                Status.PLAYING -> {
                    _state.update {
                        it.copy(
                            playState = PlayState.PLAYING,
                            isLoading = false
                        )
                    }
                }

                Status.PAUSED -> {
                    _state.update {
                        it.copy(
                            playState = PlayState.PAUSED,
                            isLoading = false
                        )
                    }
                }

                Status.INIT -> {

                }

                Status.NOT_SPECIFIED -> {}
                Status.OPENING -> {
                    _state.update {
                        it.copy(
                            isLoading = true
                        )
                    }
                }

                Status.OPENED -> {
                    _state.update {
                        it.copy(
                            duration = mediaPlayer.duration.toMillis(),
                            isLoading = false
                        )
                    }

                    mediaPlayer.play()

                    if (isReady.value) {
                        isReady.value = false
                        pendingReadyCallback?.invoke()
                        pendingReadyCallback = null
                    }
                }

                Status.STOPPED -> {
                    _state.update {
                        it.copy(
                            playState = PlayState.STOPPED,
                            isLoading = false
                        )
                    }
                }
                Status.RESUMED -> {
                    _state.update {
                        it.copy(
                            playState = PlayState.PLAYING,
                            isLoading = false
                        )
                    }
                }

                Status.SEEKING -> {
                    _state.update {
                        it.copy(
                            isLoading = true
                        )
                    }
                }

                Status.BUFFERING -> {
                    _state.update {
                        it.copy(
                            isLoading = true
                        )
                    }
                }

                Status.SEEKED -> {
                    _state.update {
                        it.copy(
                            isLoading = false
                        )
                    }

                }

                Status.EOM -> {
                    next(true)
                }

                Status.PAN -> {}
                Status.GAIN -> {}
            }
        }
    }

    private val mediaPlayer: StreamPlayer = StreamPlayer().apply {
        addStreamPlayerListener(listener)
    }
    /**
     * 准备播放指定的媒体源
     */
    private fun prepare(media: Track) {
        scope.launch {
            release()
            mediaPlayer.open(URI(media.uri ?: return@launch))
        }
    }


    /**
     * 清理当前播放器
     */
    private fun cleanupCurrentPlayer() {
        mediaPlayer.stop()
    }


    /**
     * 跳转到指定位置
     * @param millis 目标位置
     * @param seekOver 跳转完成回调
     */
    override fun seek(millis: Long, seekOver: () -> Unit) {
        scope.launch {
            mediaPlayer.seekTo((millis / 1000.0).toInt())
            seekOver()
        }
    }

    /**
     * 释放当前媒体播放器资源
     */
    override fun release() {
        cleanupCurrentPlayer()
    }

    override fun queue(medias: List<Track>, prepareOver: () -> Unit) {
        _state.update { it.copy(queue = medias) }
        prepareOver()
        playMode(state.value.playMode)
    }

    override fun playState(state: PlayState) {
        when (state) {
            PlayState.PLAYING -> mediaPlayer.resume()
            PlayState.PAUSED -> mediaPlayer.pause()
            PlayState.STOPPED -> mediaPlayer.stop()
        }
    }

    override fun playMode(mode: PlayMode) {
        _state.update { it.copy(playMode = mode) }
    }

    override fun volume(volume: Float, isMuted: Boolean) {
        mediaPlayer.setGain(volume.toDouble())
        mediaPlayer.mute = isMuted
        _state.update {
            it.copy(
                volume = volume,
                isMute = isMuted
            )
        }
    }


    override fun nextTrack(): Track? = state.value.nextTrackCandidate()

    override fun next(isAudioCompleted: Boolean) {
        when (_state.value.playMode) {
            is PlayMode.Shuffle, is PlayMode.Loop -> {
                nextTrack()?.let { play(it, isAudioCompleted) }
            }

            is PlayMode.RepeatOne -> seek(0) {}
        }
    }

    override fun previousTrack(): Track? = state.value.previousTrackCandidate()

    override fun previous() {
        when (_state.value.playMode) {
            is PlayMode.Shuffle, is PlayMode.Loop -> {
                previousTrack()?.let { play(it, false) }
            }

            is PlayMode.RepeatOne -> seek(0) {}
        }
    }

    private fun getCurrentPlayList(): List<Track> = _state.value.currentList

    override fun play(media: Track) {
        play(media, isAudioCompleted = false)
    }

    private fun play(media: Track, isAudioCompleted: Boolean = false) {
        val list = getCurrentPlayList()
        _state.update {
            it.copy(
                index = list.indexOfFirst { track -> track.id == media.id },
                current = media,
                isLoading = true
            )
        }

        scope.launch {
            if (isAudioCompleted) {
                _event.emit(
                    PlaybackEvent.TrackChanged(
                        media,
                        isAudioCompleted = true,
                        source = EventSource.Player
                    )
                )
            }
            val trackUriResolver = trackUriResolverFactory.get(media.source)
            val uri = trackUriResolver.resolve(media)
            val media = media.copy(uri = uri)
            prepare(media)
        }
    }

    override fun sync(syncState: SyncState) {
        val medias = syncState.trackList ?: return
        val current = state.value.current
        val index = syncState.track?.let { track ->
            medias.indexOfFirst { track.id == it.id }
        }

        _state.update {
            it.copy(
                queue = medias,
                index = index ?: it.index,
                current = syncState.track ?: it.current,
                playMode = syncState.playMode ?: it.playMode,
                playState = syncState.playState ?: it.playState,
                position = syncState.position ?: it.position,
            )
        }

        syncState.playMode?.let { playMode(it) }

        if (syncState.track?.id != current?.id) {
            handleTrackChange(syncState)
        } else {
            syncState.position?.let { position ->
                seek(position) {
                    syncState.playState?.let { state -> playState(state) }
                }
            }
        }
    }

    private fun handleTrackChange(syncState: SyncState) {
        isReady.value = true
        play(syncState.track ?: return, isAudioCompleted = false)
        pendingReadyCallback = {
            syncState.position?.let { position ->
                seek(position) {
                    syncState.playState?.let { state -> playState(state) }
                }
            }
        }
    }

    override fun addTrack(media: Track, index: Int) {
        _state.update { it.withTrackAdded(media, index) }
    }

    override fun removeTrack(index: Int) {
        _state.update { it.withTrackRemoved(index) }
    }
}

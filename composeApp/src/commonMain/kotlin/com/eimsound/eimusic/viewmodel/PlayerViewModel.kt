package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.data.Storage
import com.eimsound.eimusic.media.MediaPlayerController
import com.eimsound.eimusic.media.MediaPlayerListener
import com.eimsound.eimusic.media.PlayMode
import com.eimsound.eimusic.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerState(
    val isLoading: Boolean = false,
    val position: Duration = Duration(0),
    val playMode: PlayMode = PlayMode.LOOP,
    val volume: Double = 1.0,
    val isMute: Boolean = false,
    val isPlaying: Boolean = false,
    val duration: Duration? = null
)

class PlayerViewModel(
    private val storage: Storage,
    private val controller: MediaPlayerController
) : ViewModel() {
    private val _state = MutableStateFlow(
        PlayerState(
            position = controller.position ?: Duration(0),
            playMode = PlayMode.valueOf(storage.get(Settings::playMode, PlayMode.LOOP.name)),
            volume = storage.get(Settings::volume, 1.0),
            isMute = storage.get(Settings::isMuted, false),
            isPlaying = controller.isPlaying,
            duration = controller.duration
        )
    )
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    fun isLoading(value: Boolean) {
        _state.value = _state.value.copy(isLoading = value)
    }

    fun isPlaying(value: Boolean) {
        _state.value = _state.value.copy(isPlaying = value)
        if (value) {
            controller.start()
        } else {
            controller.pause()
        }
    }

    fun seek(value: Duration, seekOver: () -> Unit = {}) {
        _state.value = _state.value.copy(position = value)
        controller.seek(value, seekOver)
    }

    fun onPlayModeChanged(value: PlayMode) {
        _state.value = _state.value.copy(playMode = value)
        storage.save(Settings::playMode, value.name)
    }

    fun onVolumeChanged(value: Double) {
        _state.value = _state.value.copy(volume = value)
        controller.volume = value
        storage.save(Settings::volume, value)
    }

    fun onIsMuteChanged(value: Boolean) {
        _state.value = _state.value.copy(isMute = value)
        controller.isMuted = value
        storage.save(Settings::isMuted, value)
    }

    fun play(
        trackUri: String,
        playingListViewModel: PlayingListViewModel,
    ) {
        _state.value = _state.value.copy(isLoading = true)
        controller.prepare(trackUri, listener = object : MediaPlayerListener {
            override fun onReady() {
                _state.value = _state.value.copy(isLoading = false)
                controller.volume = _state.value.volume
                controller.isMuted = _state.value.isMute
                val newState = _state.value.copy(
                    duration = controller.duration,
                    isPlaying = true
                )
                _state.value = newState
                controller.start()
            }

            override fun onAudioCompleted() {
                when (_state.value.playMode) {
                    PlayMode.LOOP -> playingListViewModel.next(_state.value.playMode)
                    PlayMode.REPEAT_ONE -> {
                        controller.seek(Duration(0))
                        controller.start()
                    }

                    PlayMode.SHUFFLE -> {
                        playingListViewModel.next(_state.value.playMode)
                    }
                }
            }

            override fun onError() {
                playingListViewModel.next(_state.value.playMode)
            }

            override fun timer(duration: Duration) {
                _state.value = _state.value.copy(position = duration)
            }

            override fun onLoading() {
                _state.value = _state.value.copy(isLoading = true)
            }

            override fun onLoaded() {
                _state.value = _state.value.copy(isLoading = false)
            }
        })
    }

    fun release() {
        controller.release()
    }
}
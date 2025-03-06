package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.data.PlayerUiState
import com.eimsound.eimusic.media.PlayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update


class PlayerViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        PlayerUiState()
    )
    val state: StateFlow<PlayerUiState> = _state

    fun loading(isLoading: Boolean) {
        _state.update {
            state.value.copy(isLoading = isLoading)
        }
    }

    fun play() {
        _state.update {
            state.value.copy(isPlaying = true)
        }
    }

    fun pause() {
        _state.update {
            state.value.copy(isPlaying = false)
        }
    }

    fun seek(position: Duration) {
        _state.update {
            state.value.copy(position = position)
        }
    }

    fun onPlayModeChanged(playMode: PlayMode) {
        _state.update {
            state.value.copy(playMode = playMode)
        }
    }

    fun onVolumeChanged(volume: Double) {
        _state.update {
            state.value.copy(volume = volume)
        }
    }

    fun onIsMuteChanged(isMute: Boolean) {
        _state.update {
            state.value.copy(isMute = isMute)
        }
    }

//    val state: StateFlow<PlayerState> = _state


//    val trackList = mutableStateOf<List<Item>>(emptyList())
//    val shuffleList = mutableStateOf<MutableSet<Item>>(mutableSetOf())
//    val selectedIndex = mutableStateOf(0)
//    val isLoading = mutableStateOf(false)
//    val position = mutableStateOf(0.0f)
//    val playMode = mutableStateOf(PlayMode.LOOP)
//    val volume = mutableStateOf(mediaPlayerController.volume)
//    val isMute = mutableStateOf(mediaPlayerController.isMuted)
//    val isPlaying = mutableStateOf(mediaPlayerController.isPlaying)
//    val selectedTrack = mutableStateOf(trackList.value.getOrNull(selectedIndex.value))
}

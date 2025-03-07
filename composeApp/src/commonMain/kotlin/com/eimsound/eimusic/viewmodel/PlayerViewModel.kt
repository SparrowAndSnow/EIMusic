package com.eimsound.eimusic.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.media.PlayMode


class PlayerViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    var position by mutableStateOf(Duration(0))
        private set
    var playMode by mutableStateOf(PlayMode.LOOP)
        private set
    var volume by mutableStateOf(1.0)
        private set
    var isMute by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    fun isLoading(value: Boolean) {
        isLoading = value
    }

    fun isPlay(value: Boolean) {
        isPlaying = value
    }

    fun seek(value: Duration) {
        position = value
    }

    fun onPlayModeChanged(value: PlayMode) {
        playMode = value
    }

    fun onVolumeChanged(value: Double) {
        volume = value
    }

    fun onIsMuteChanged(value: Boolean) {
        isMute = value
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

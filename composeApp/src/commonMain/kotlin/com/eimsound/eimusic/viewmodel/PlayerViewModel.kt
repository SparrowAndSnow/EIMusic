package com.eimsound.eimusic.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.media.MediaPlayerController
import com.eimsound.eimusic.media.MediaPlayerListener
import com.eimsound.eimusic.media.PlayMode


class PlayerViewModel(private val controller: MediaPlayerController) : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    var position by mutableStateOf(controller.position?: Duration(0))
        private set
    var playMode by mutableStateOf(PlayMode.LOOP)
        private set
    var volume by mutableStateOf(controller.volume)
        private set
    var isMute by mutableStateOf(controller.isMuted)
        private set
    var isPlaying by mutableStateOf(controller.isPlaying)
        private set
    var duration by mutableStateOf(controller.duration)
        private set

    fun isLoading(value: Boolean) {
        isLoading = value
    }

    fun isPlaying(value: Boolean) {
        isPlaying = value
        if (value) {
            controller.start()
        } else {
            controller.pause()
        }
    }

    fun seek(value: Duration, seekOver: () -> Unit = {}) {
        position = value
        controller.seek(value, seekOver)
    }

    fun onPlayModeChanged(value: PlayMode) {
        playMode = value
    }

    fun onVolumeChanged(value: Double) {
        volume = value
        controller.volume = volume
    }

    fun onIsMuteChanged(value: Boolean) {
        isMute = value
        controller.isMuted = value
    }

    fun play(
        trackUri: String,
        playingListViewModel: PlayingListViewModel,
    ) {
        isLoading = true
        controller.prepare(trackUri, listener = object : MediaPlayerListener {
            override fun onReady() {
                isLoading = false
                controller.volume = volume
                controller.isMuted = isMute
                duration = controller.duration
                controller.start()
                isPlaying = true
            }

            override fun onAudioCompleted() {
                when (playMode) {
                    PlayMode.LOOP -> playingListViewModel.next(playMode)
                    PlayMode.REPEAT_ONE -> {
                        controller.seek(Duration(0))
                        controller.start()
                    }

                    PlayMode.SHUFFLE -> {
//                        vm.state.value.shuffleList.removeIf { shuffleList.value.contains(selectedTrack.value) }
                        playingListViewModel.next(playMode)
                    }
                }

            }

            override fun onError() {
                playingListViewModel.next(playMode)
            }

            override fun timer(value: Duration) {
                position = value
            }

            override fun onLoading() {
                isLoading = true
            }

            override fun onLoaded() {
                isLoading = false
            }
        })
    }

    fun release() {
        controller.release()
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

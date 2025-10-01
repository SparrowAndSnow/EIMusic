package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.events.TrackListEvent
import com.eimsound.eimusic.media.PlayMode
import com.eimsound.eimusic.media.PlaybackController
import com.eimsound.eimusic.music.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class PlayingListState(
    val trackList: List<Track> = emptyList(),
    val shuffleList: List<Track> = emptyList(),
    val selectedIndex: Int = 0,
    val selectedTrack: Track? = null
)

class PlayingListViewModel() : ViewModel(), PlaybackController {
    private val _state = MutableStateFlow(PlayingListState())
    val state: StateFlow<PlayingListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            EventBus.PlaybackEventBus.receive {
                when (it) {
                    is PlaybackEvent.Previous -> {
                        previous(it.playMode)
                    }

                    is PlaybackEvent.Next -> {
                        next(it.playMode)
                    }
                    is PlaybackEvent.PlaybackModeChanged -> {}
                    is PlaybackEvent.TogglePlaybackPause -> {}
                }
            }
        }
        viewModelScope.launch {
            EventBus.TrackListEventBus.receive {
                when(it){
                    is TrackListEvent.PlayTrackList -> {
                        // 如果点击的音乐不在当前播放列表中，则重新加载播放列表
                        if (_state.value.trackList != it.tracks) {
                            loadAndPlay(it.tracks, it.track)
                        } else {
                            // 如果在当前播放列表中，直接播放
                            play(it.track)
                        }
                    }
                    is TrackListEvent.AddedToQueue -> {
                        addTrack(it.track)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun onTrackChanged(track: Track) {
        EventBus.PlayingListEventBus.send(PlayingListEvent.TrackChanged(track))
    }

    override fun load(tracks: List<Track>) {
        _state.value = PlayingListState(
            trackList = tracks,
            selectedIndex = 0,
            selectedTrack = tracks.firstOrNull()
        )
    }

    override fun play(track: Track) {
        val currentState = _state.value
        val index = currentState.trackList.indexOf(track)
        if (index != -1) {
            _state.value = currentState.copy(
                selectedIndex = index,
                selectedTrack = track
            )
            // 发送播放事件而不是直接调用playbackController
            onTrackChanged(track)
        }
    }

    fun loadAndPlay(tracks: List<Track>, track: Track) {
        _state.value = PlayingListState(
            trackList = tracks,
            selectedIndex = tracks.indexOf(track),
            selectedTrack = track
        )
        // 发送播放事件而不是直接调用playbackController
        onTrackChanged(track)
    }

    fun addTrack(track: Track) {
        val currentState = _state.value
        val newList = currentState.trackList + track
        _state.value = currentState.copy(
            trackList = newList
        )
    }

    fun removeTrack(track: Track) {
        val currentState = _state.value
        val newList = currentState.trackList.filter { it != track }
        val newSelectedTrack = if (currentState.selectedTrack == track) {
            newList.firstOrNull()
        } else {
            currentState.selectedTrack
        }
        val newIndex = if (newSelectedTrack != null) {
            newList.indexOf(newSelectedTrack)
        } else {
            0
        }
        _state.value = currentState.copy(
            trackList = newList,
            selectedTrack = newSelectedTrack,
            selectedIndex = newIndex
        )
    }

    fun clear() {
        _state.value = PlayingListState()
    }

    fun updateShuffleList() {
        val currentState = _state.value
        if (currentState.trackList.isNotEmpty()) {
            val shuffled = currentState.trackList.shuffled(Random)
            _state.value = currentState.copy(
                shuffleList = shuffled
            )
        }
    }

    override fun next(playMode: PlayMode) {
        val currentState = _state.value
        // 先检查并更新 shuffleList
        if (playMode == PlayMode.SHUFFLE && currentState.shuffleList.isEmpty() && currentState.trackList.isNotEmpty()) {
            updateShuffleList()
        }

        // 重新获取最新的状态
        val updatedState = _state.value
        val list = if (playMode == PlayMode.SHUFFLE)
            updatedState.shuffleList
        else
            updatedState.trackList

        if (list.isEmpty()) return

        val newIndex = if (updatedState.selectedIndex < list.size - 1) {
            updatedState.selectedIndex + 1
        } else {
            0
        }
        if (newIndex < list.size) {
            val track = list[newIndex]

            _state.value = updatedState.copy(
                selectedIndex = newIndex,
                selectedTrack = track
            )
            // 发送播放事件而不是直接调用playbackController
            onTrackChanged(track)
        }
    }

    override fun previous(playMode: PlayMode) {
        val currentState = _state.value
        // 先检查并更新 shuffleList
        if (playMode == PlayMode.SHUFFLE && currentState.shuffleList.isEmpty() && currentState.trackList.isNotEmpty()) {
            updateShuffleList()
        }

        // 重新获取最新的状态
        val updatedState = _state.value
        val list = if (playMode == PlayMode.SHUFFLE)
            updatedState.shuffleList
        else
            updatedState.trackList

        if (list.isEmpty()) return

        val newIndex = if (updatedState.selectedIndex > 0) {
            updatedState.selectedIndex - 1
        } else {
            list.size - 1
        }

        if (newIndex < list.size) {
            val track = list[newIndex]
            _state.value = updatedState.copy(
                selectedIndex = newIndex,
                selectedTrack = track
            )
            // 发送播放事件而不是直接调用playbackController
            onTrackChanged(track)
        }
    }
}
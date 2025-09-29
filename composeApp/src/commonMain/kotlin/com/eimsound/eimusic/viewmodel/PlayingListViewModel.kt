package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.media.PlayMode
import com.eimsound.eimusic.music.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class PlayingListState(
    val trackList: List<Track> = emptyList(),
    val shuffleList: List<Track> = emptyList(),
    val selectedIndex: Int = 0,
    val selectedTrack: Track? = null
)

class PlayingListViewModel : ViewModel() {
    private val _state = MutableStateFlow(PlayingListState())
    val state: StateFlow<PlayingListState> = _state.asStateFlow()

    fun load(list: List<Track>) {
        _state.value = PlayingListState(
            trackList = list,
            selectedIndex = 0,
            selectedTrack = list.firstOrNull()
        )
    }

    fun play(track: Track) {
        val currentState = _state.value
        val index = currentState.trackList.indexOf(track)
        if (index != -1) {
            _state.value = currentState.copy(
                selectedIndex = index,
                selectedTrack = track
            )
        }
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

    fun next(playMode: PlayMode) {
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
            _state.value = updatedState.copy(
                selectedIndex = newIndex,
                selectedTrack = list[newIndex]
            )
        }
    }

    fun previous(playMode: PlayMode) {
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
            _state.value = updatedState.copy(
                selectedIndex = newIndex,
                selectedTrack = list[newIndex]
            )
        }
    }
}
package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.EventSource
import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.storage.IStorage


class PlayingListViewModel(
    private val storage: IStorage,
    private val controller: IMediaPlayerController
) : ViewModel() {
    val state = controller.state
//    fun sendTrackListChanged(tracks: List<Track>, track: Track?, source: EventSource = EventSource.User) {
//        if (tracks.isEmpty()) return
//        EventBus.PlayingListEventBus.send(
//            PlayingListEvent.TrackListChanged(
//                tracks = tracks,
//                track = track ?: tracks.first(),
//                source = source,
//                targets = setOf(EventSource.Player, EventSource.Remote)
//            )
//        )
//    }

    fun sendTrackChanged(track: Track, isAudioCompleted: Boolean = false, source: EventSource = EventSource.User) {
        EventBus.PlaybackEventBus.send(
            PlaybackEvent.TrackChanged(
                track,
                isAudioCompleted = isAudioCompleted,
                autoPlay = true,
                source = source,
                targets = setOf(EventSource.Player, EventSource.Remote)
            )
        )
    }

    fun sendRemoveTrack(track: Track, source: EventSource = EventSource.User) {
        if (source == EventSource.User) {
            val index = state.value.queue.indexOfFirst { it.id == track.id }
            if (index >= 0) controller.removeTrack(index)
        }
        EventBus.PlayingListEventBus.send(PlayingListEvent.TrackRemoved(track, source = source))
    }

    fun sendMoveTrack(from: Int, to: Int, source: EventSource = EventSource.User) {
        if (source == EventSource.User) controller.moveTrack(from, to)
        EventBus.PlayingListEventBus.send(PlayingListEvent.TrackMoved(from, to, source = source))
    }

    fun sendClearQueue(source: EventSource = EventSource.User) {
        EventBus.PlayingListEventBus.send(PlayingListEvent.TrackListChanged(emptyList(), null, source = source))
    }

    fun scroll(doScroll: (Int) -> Unit) {
        if (state.value.current != null && state.value.queue.isNotEmpty()) {
            val index = state.value.queue.indexOfFirst { state.value.current?.id == it.id }
            if (index != -1) {
                doScroll(index)
            }
        }
    }

//    fun load(tracks: List<Track>) {
//        _state.value = PlayingListState(
//            trackList = tracks,
//            selectedIndex = 0,
//            selectedTrack = tracks.firstOrNull()
//        )
//    }

//    fun play(track: Track): Boolean {
//        val currentState = _state.value
//        val index = currentState.trackList.indexOf(track)
//        if (index != -1) {
//            _state.value = currentState.copy(
//                selectedIndex = index,
//                selectedTrack = track
//            )
//            return true
//        }
//        return false
//    }

//    fun loadTracks(tracks: List<Track>, track: Track? = null) {
//        _state.value = PlayingListState(
//            trackList = tracks,
//            selectedIndex = tracks.indexOf(track),
//            selectedTrack = track
//        )
//    }

//    fun sendTrackAdded(track: Track, strategy: AddTrackStrategy) {
//        if (state.value.queue.any { it.id == track.id }) {
//            viewModelScope.launch {
//                Toast.show(ToastMessage("歌曲已存在"))
//            }
//            return
//        }
//        EventBus.PlayingListEventBus.send(PlayingListEvent.TrackAdded(track, strategy, source = EventSource.User))
//    }


//    fun addTrack(track: Track) {
//        if (track in _state.value.trackList) return
//        val currentState = _state.value
//        val newList = currentState.trackList + track
//        _state.value = currentState.copy(
//            trackList = newList
//        )
//    }


//    fun clear() {
//        _state.value = PlayingListState()
//    }

//    fun updateShuffleList() {
//        val currentState = _state.value
//        if (currentState.trackList.isNotEmpty()) {
//            val shuffled = currentState.trackList.shuffled(Random)
//            _state.value = currentState.copy(
//                shuffleList = shuffled
//            )
//        }
//    }

//    fun next(): Track? {
//        val currentState = _state.value
//        // 先检查并更新 shuffleList
//        if (currentState.playMode == PlayMode.SHUFFLE && currentState.shuffleList.isEmpty() && currentState.trackList.isNotEmpty()) {
//            updateShuffleList()
//        }
//
//        // 重新获取最新的状态
//        val updatedState = _state.value
//        val list = if (currentState.playMode == PlayMode.SHUFFLE)
//            updatedState.shuffleList
//        else
//            updatedState.trackList
//
//        if (list.isEmpty()) return null
//
//        val newIndex = (updatedState.selectedIndex + 1).cycle(0, list.size - 1)
//
//        val track = list[newIndex]
//
//        _state.value = updatedState.copy(
//            selectedIndex = newIndex,
//            selectedTrack = track
//        )
//        return track
//    }
//
//    fun previous(): Track? {
//        val currentState = _state.value
//        // 先检查并更新 shuffleList
//        if (currentState.playMode == PlayMode.SHUFFLE && currentState.shuffleList.isEmpty() && currentState.trackList.isNotEmpty()) {
//            updateShuffleList()
//        }
//
//        // 重新获取最新的状态
//        val updatedState = _state.value
//        val list = if (currentState.playMode == PlayMode.SHUFFLE)
//            updatedState.shuffleList
//        else
//            updatedState.trackList
//
//        if (list.isEmpty()) return null
//
//        val newIndex = (updatedState.selectedIndex - 1).cycle(0, list.size - 1)
//
//        val track = list[newIndex]
//        _state.value = updatedState.copy(
//            selectedIndex = newIndex,
//            selectedTrack = track
//        )
//        return track
//    }


}
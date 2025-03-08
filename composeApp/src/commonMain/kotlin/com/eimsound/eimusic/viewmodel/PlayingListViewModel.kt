package com.eimsound.eimusic.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.media.PlayMode
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.network.models.topfiftycharts.Item

class PlayingListViewModel : ViewModel() {
    var trackList by mutableStateOf<List<Track>>(emptyList())
        private set
    var shuffleList by mutableStateOf<MutableSet<Track>>(mutableSetOf())
        private set
    var selectedIndex by mutableStateOf(0)
        private set
    var selectedTrack by mutableStateOf<Track?>(null)
        private set

    fun load(list: List<Track>) {
        trackList = list
        selectedIndex = 0
        selectedTrack = list.firstOrNull()
    }

    fun next(playMode: PlayMode) {
        val list = if (playMode == PlayMode.SHUFFLE)
            shuffleList
        else
            trackList

        if (list.isEmpty()) return

        if (selectedIndex < list.size - 1) {
            val index = selectedIndex + 1
            selectedIndex = index
            selectedTrack = list.toList()[index]
        } else {
            selectedIndex = 0
            selectedTrack = list.toList()[0]
        }
    }

    fun previous(playMode: PlayMode) {
        val list = if (playMode == PlayMode.SHUFFLE)
            shuffleList
        else
            trackList

        if (list.isEmpty()) return

        if (selectedIndex - 1 >= 0) {
            val index = selectedIndex - 1
            selectedIndex = index
            selectedTrack = list.toList()[index]
        } else {
            selectedIndex = list.size - 1
            selectedTrack = list.toList()[list.size - 1]
        }
    }
}

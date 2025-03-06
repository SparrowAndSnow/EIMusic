package com.eimsound.eimusic.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.media.PlayMode
import com.eimsound.eimusic.network.models.topfiftycharts.Item

class PlayingListViewModel : ViewModel() {
    val trackList = mutableStateOf<List<Item>>(emptyList())
    val shuffleList = mutableStateOf<MutableSet<Item>>(mutableSetOf())
    val selectedIndex = mutableStateOf<Int>(0)
    val selectedTrack = mutableStateOf<Item?>(null)

    fun next(playMode: PlayMode) {
        val list = if (playMode == PlayMode.SHUFFLE)
            shuffleList.value
        else
            trackList.value

        if (selectedIndex.value < list.size - 1) {
            val index = selectedIndex.value + 1
            selectedIndex.value = index
            selectedTrack.value = list.toList()[index]
        }else{
            selectedIndex.value = 0
            selectedTrack.value = list.toList()[0]
        }
    }

    fun previous(playMode: PlayMode) {
        val list = if (playMode == PlayMode.SHUFFLE)
            shuffleList.value
        else
            trackList.value

        if (selectedIndex.value - 1 >= 0) {
            val index = selectedIndex.value - 1
            selectedIndex.value = index
            selectedTrack.value = list.toList()[index]
        }else{
            selectedIndex.value = list.size - 1
            selectedTrack.value = list.toList()[list.size - 1]
        }
    }
}

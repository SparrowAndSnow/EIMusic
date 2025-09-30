package com.eimsound.eimusic.components


import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlayingList() {
    val trackListViewModel = koinViewModel<PlayingListViewModel>()
    val playingListState by trackListViewModel.state.collectAsState()
    val lazyListState = rememberLazyListState()

    ColumnList(state = lazyListState, list = playingListState.trackList, key = Track::id) {
        TrackItem(track = it, isPlaying = it == playingListState.selectedTrack, onPlayClick = {
            trackListViewModel.play(it)
        })
    }
}
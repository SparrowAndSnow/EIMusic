package com.eimsound.eimusic.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.eimsound.eimusic.components.ColumnList
import com.eimsound.eimusic.components.TrackItem
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.util.loadTrackFiles
import com.eimsound.eimusic.viewmodel.LocalViewModel
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
object LocalRoute

@Composable
fun LocalView() {
    val localViewModel = koinViewModel<LocalViewModel>()
    val trackListViewModel = koinViewModel<PlayingListViewModel>()
    val playingListState by trackListViewModel.state.collectAsState()

    LaunchedEffect(localViewModel.localPaths){
        loadTrackFiles(localViewModel.localPaths)
    }

    ColumnList(
        list = localViewModel.tracks,
        key = Track::id
    ) {
        TrackItem(track = it, isPlaying = it == playingListState.selectedTrack, onPlayClick = {
            trackListViewModel.play(it)
        })
    }
}

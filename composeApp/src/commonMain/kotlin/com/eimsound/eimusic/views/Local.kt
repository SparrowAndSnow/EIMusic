package com.eimsound.eimusic.views

import androidx.compose.runtime.Composable
import com.eimsound.eimusic.components.ColumnList
import com.eimsound.eimusic.components.TrackItem
import com.eimsound.eimusic.music.Track
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
    ColumnList(
        list = localViewModel.tracks,
        key = Track::id
    ) {
        TrackItem(track = it, isPlaying = it == trackListViewModel.selectedTrack, onPlayClick = {
            trackListViewModel.play(it)
        })
    }
}

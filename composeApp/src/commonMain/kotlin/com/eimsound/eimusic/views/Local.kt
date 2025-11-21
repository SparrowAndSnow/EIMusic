package com.eimsound.eimusic.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.eimsound.eimusic.components.TrackList
import com.eimsound.eimusic.viewmodel.LocalViewModel
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun LocalView() {
    val localViewModel = koinViewModel<LocalViewModel>()
    val tracks by localViewModel.tracks.collectAsState()
    TrackList(tracks)
}
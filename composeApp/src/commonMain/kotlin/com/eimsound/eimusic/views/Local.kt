package com.eimsound.eimusic.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun LocalView(playingListViewModel: PlayingListViewModel) {
    val localViewModel = koinViewModel<LocalViewModel>()
    val playingListState by playingListViewModel.state.collectAsState()
    val tracks by localViewModel.tracks.collectAsState()
    
    ColumnList(
        list = tracks,
        key = Track::id
    ) {
        TrackItem(track = it, isPlaying = it == playingListState.selectedTrack, onPlayClick = {
            // 如果点击的音乐不在当前播放列表中，则重新加载播放列表
            if (playingListState.trackList != tracks) {
                playingListViewModel.load(tracks)
            }
            playingListViewModel.play(it)
        })
    }
}
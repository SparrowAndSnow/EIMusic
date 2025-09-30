package com.eimsound.eimusic.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.components.ColumnList
import com.eimsound.eimusic.components.TrackItem
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import com.eimsound.eimusic.viewmodel.WelcomeViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.serialization.Serializable

@Serializable
object WelcomeRoute

@Composable
fun WelcomeView(playingListViewModel: PlayingListViewModel) {
    val viewModel: WelcomeViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val playingListState by playingListViewModel.state.collectAsState()
    val tracks = uiState.tracks

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...")
            }
        }
        
        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${uiState.error}")
            }
        }
        
        uiState.isEmpty -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No tracks available")
            }
        }
        
        else -> {
            ColumnList(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                list = uiState.tracks,
                key = Track::id
            ) {
                TrackItem(
                    track = it,
                    isPlaying = it == playingListState.selectedTrack,
                    onPlayClick = {
                        // 如果点击的音乐不在当前播放列表中，则重新加载播放列表
                        if (playingListState.trackList != tracks) {
                            playingListViewModel.load(tracks)
                        }
                        playingListViewModel.play(it)
                    },
                    onTrackNameClick = {
                        // Handle track name click
                    },
                    onArtistClick = { 
                        // Handle artist click
                    },
                    onAddToPlaylist = {
                        playingListViewModel.addTrack(it)
                    },
                    onFavorite = {
                        // Handle favorite action
                    },
                    onDownload = {
                        // Handle download action
                    },
                    onShare = {
                        // Handle share action
                    }
                )
            }
        }
    }
}
package com.eimsound.eimusic.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.my_location_fill_24px
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlayingList() {
    val playingListViewModel = koinViewModel<PlayingListViewModel>()
    val state by playingListViewModel.state.collectAsState()
    val lazyListState = rememberLazyListState()
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
            )
            IconButton(onClick = {
                playingListViewModel.scroll {
                    scope.launch {
                        lazyListState.animateScrollToItem(it)
                    }
                }
            }) {
                Icon(
                    painter = painterResource(Res.drawable.my_location_fill_24px),
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }
        }
        ColumnList(state = lazyListState, list = state.queue, key = Track::id) {
            TrackItem(
                track = it,
                isPlaying = it.id == state.current?.id,
                onPlayClick = {
                    playingListViewModel.sendTrackChanged(it)
                },
                showAddToQueue = false,
                showRemoveFromQueue = state.current?.id != it.id,
                onRemoveFromPlaylist = { track ->
                    playingListViewModel.sendRemoveTrack(track)
                }
            )
        }
    }
}
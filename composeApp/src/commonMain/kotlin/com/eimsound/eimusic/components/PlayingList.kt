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
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.viewmodel.MyViewModel
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import com.eimsound.eimusic.views.components.my.LocalPlaylistPickerDialog
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.delete_24px
import eimusic.composeapp.generated.resources.my_location_fill_24px
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlayingList() {
    val playingListViewModel = koinViewModel<PlayingListViewModel>()
    val state by playingListViewModel.state.collectAsState()
    val myViewModel = koinViewModel<MyViewModel>()
    val myState by myViewModel.state.collectAsState()
    var pickerTrack by remember { mutableStateOf<Track?>(null) }
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
            Row {
                IconButton(onClick = { playingListViewModel.sendClearQueue() }) {
                    Icon(
                        painter = painterResource(Res.drawable.delete_24px),
                        contentDescription = "Clear queue",
                        tint = palette.textSecondary
                    )
                }
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
                },
                onAddToLibraryPlaylist = { pickerTrack = it },
                showMoveOptions = true,
                canMoveUp = state.queue.indexOf(it) > 0,
                canMoveDown = state.queue.indexOf(it) in 0 until state.queue.lastIndex,
                onMoveUp = {
                    val index = state.queue.indexOf(it)
                    if (index > 0) playingListViewModel.sendMoveTrack(index, index - 1)
                },
                onMoveDown = {
                    val index = state.queue.indexOf(it)
                    if (index in 0 until state.queue.lastIndex) playingListViewModel.sendMoveTrack(index, index + 1)
                },
            )
        }
    }

    LocalPlaylistPickerDialog(
        visible = pickerTrack != null,
        playlists = myState.playlists.filter { it.id.source == TrackSource.LOCAL },
        onDismiss = { pickerTrack = null },
        onSelect = { playlist ->
            pickerTrack?.let { myViewModel.addTrackToPlaylist(playlist.id.id, it) }
            pickerTrack = null
        },
    )
}
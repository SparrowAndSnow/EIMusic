package com.eimsound.eimusic.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.eimsound.eimusic.layout.SidebarComponent
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.viewmodel.DefaultLayoutViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Preview
@Composable
actual fun PlayerBar() {
//    val playerViewModel = koinViewModel<PlayerViewModel>()
//    val defaultLayoutViewModel = koinViewModel<DefaultLayoutViewModel>()
//
//    val playerState by playerViewModel.state.collectAsState()
//    val sideBarState by defaultLayoutViewModel.sideBarState.collectAsState()
//
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(64.dp)
//            .clip(MaterialTheme.shapes.medium)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .align(Alignment.Center)
//                .padding(horizontal = 8.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Track Info
//            Row(
//                modifier = Modifier.weight(1f),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                TrackImage(
//                    modifier = Modifier
//                        .size(48.dp),
//                    selectedTrack = playerState.track, isLoading = playerState.isLoading
//                )
//                Column(
//                    modifier = Modifier
//                        .padding(start = 8.dp)
//                        .weight(1f)
//                ) {
//                    Text(
//                        text = playerState.track?.name.orEmpty(),
//                        style = MaterialTheme.typography.bodyMedium,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                    Text(
//                        text = playerState.track?.artists?.joinToString(",") { it.name.toString() }
//                            .orEmpty(),
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                }
//            }
//
//            // Playback Controls
//            Row(
//                modifier = Modifier.weight(1f),
//                horizontalArrangement = Arrangement.Center,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                IconButton(onClick = playerViewModel::previous) {
//                    Icon(
//                        imageVector = Icons.Default.SkipPrevious,
//                        contentDescription = "Previous",
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
//
//                IconButton(
//                    onClick = { playerViewModel.isPlaying(!playerState.isPlaying) },
//                    modifier = Modifier
//                        .padding(horizontal = 8.dp)
//                        .size(36.dp)
//                ) {
//                    Icon(
//                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
//                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
//                        modifier = Modifier.size(36.dp)
//                    )
//                }
//
//                IconButton(onClick = playerViewModel::next) {
//                    Icon(
//                        imageVector = Icons.Default.SkipNext,
//                        contentDescription = "Next",
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
//            }
//
//            // Playlist Button
//            Row(
//                modifier = Modifier.weight(1f),
//                horizontalArrangement = Arrangement.End,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                IconButton(
//                    onClick = {
//                        defaultLayoutViewModel.updateSideBar(
//                            !sideBarState.showSideBar,
//                            SidebarComponent.PLAYLIST
//                        )
//                    }
//                ) {
//                    Icon(
//                        imageVector = Icons.AutoMirrored.Default.QueueMusic,
//                        contentDescription = "Playlist",
//                        tint = if (sideBarState.showSideBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
//                    )
//                }
//            }
//        }
//
//        // Progress bar at the bottom
//        LinearProgressIndicator(
//            progress = { playerState.duration?.toPercent(playerState.position) ?: 0f },
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(2.dp)
//                .align(Alignment.BottomStart)
//        )
//    }
}

@Composable
fun TrackImage(modifier: Modifier = Modifier, selectedTrack: Track?, isLoading: Boolean) {
    val painter = rememberAsyncImagePainter(
        selectedTrack?.album?.image?.orEmpty()
    )
    Box(modifier = modifier
        .clip(RoundedCornerShape(4.dp))
        .width(64.dp)
        .height(64.dp)) {
        Image(
            painter,
            selectedTrack?.album?.image?.orEmpty(),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .width(64.dp)
                .height(64.dp),
            contentScale = ContentScale.Crop
        )
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
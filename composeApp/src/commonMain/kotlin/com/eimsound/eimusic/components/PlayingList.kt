package com.eimsound.eimusic.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.music.Artist
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.artists_divider
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlayingList() {
    val trackListViewModel = koinViewModel<PlayingListViewModel>()
    val lazyListState = rememberLazyListState()

    ColumnList(state = lazyListState, list = trackListViewModel.trackList, key = Track::id) {
        TrackItem(track = it, isPlaying = it == trackListViewModel.selectedTrack, onPlayClick = {
            trackListViewModel.play(it)
        })
    }
}

@Composable
fun TrackItem(
    modifier: Modifier = Modifier,
    track: Track,
    isPlaying: Boolean,
    onPlayClick: () -> Unit = {},
    onTrackNameClick: () -> Unit = {},
    onArtistClick: (Artist) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier.fillMaxWidth()
            .hoverable(interactionSource)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHovered) MaterialTheme.colorScheme.surfaceVariant else Color.Unspecified)
            .padding(8.dp)
    ) {
        TrackImage(image = track?.album?.image, isPlaying = isPlaying, onPlayClick = onPlayClick)
        Column(Modifier.padding(start = 8.dp)) {
            TrackName(name = track?.name.orEmpty(), onClick = onTrackNameClick)
            ArtistList(
                artists = track?.artists.orEmpty(),
                onClick = onArtistClick
            ) {
                Text(stringResource(Res.string.artists_divider), modifier = Modifier.padding(4.dp))
            }
        }
    }
}
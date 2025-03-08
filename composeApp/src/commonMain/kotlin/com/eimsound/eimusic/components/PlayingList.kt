package com.eimsound.eimusic.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlayingList() {
    val trackListViewModel = koinViewModel<PlayingListViewModel>()
    val lazyListState = rememberLazyListState()

    ColumnList(state = lazyListState, list = trackListViewModel.trackList) {
        Item(it)
    }
}


@Composable
fun Item(track: Track?) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth()
            .hoverable(interactionSource)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHovered) MaterialTheme.colorScheme.surfaceVariant else Color.Unspecified)
            .padding(8.dp)
    ) {
        TrackImage(image = track?.album?.image)
        Column(Modifier.padding(start = 8.dp)) {
            Text(
                text = track?.name.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
            )
            Text(
                text = track?.artists?.map { it.name }?.joinToString(",")
                    .orEmpty(),
                modifier = Modifier.padding(top = 8.dp)
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
            )
        }
    }
}

@Composable
fun TrackImage(modifier: Modifier = Modifier, image: String?) {
    val painter = rememberAsyncImagePainter(image)
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp)).width(64.dp).height(64.dp)) {
        Image(
            painter,
            image,
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).width(64.dp).height(64.dp),
            contentScale = ContentScale.Crop
        )
    }
}
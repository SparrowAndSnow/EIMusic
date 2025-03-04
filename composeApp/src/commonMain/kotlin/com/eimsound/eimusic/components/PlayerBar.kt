package com.eimsound.eimusic.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.eimsound.eimusic.media.MediaPlayerController
import com.eimsound.eimusic.media.MediaPlayerListener
import com.eimsound.eimusic.network.SpotifyApiImpl
import com.eimsound.eimusic.network.models.topfiftycharts.Item
import com.eimsound.eimusic.network.models.topfiftycharts.Track
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun PlayerBar(mediaPlayerController: MediaPlayerController) {
    var trackList = listOf(
        Item(null,null,null,null,null,null),
        Item(null,null,null,null,null,null),
    )

    LaunchedEffect(trackList) {
        trackList = SpotifyApiImpl().getTopFiftyChart().tracks?.items ?: listOf()
    }

    val selectedIndex = remember { mutableStateOf(0) }
    val isLoading = remember { mutableStateOf(true) }
    val selectedTrack = trackList[selectedIndex.value]


    playTrack(selectedTrack, mediaPlayerController, isLoading, selectedIndex, trackList)

    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp).clickable(
                indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val painter = rememberAsyncImagePainter(
                selectedTrack.track?.album?.images?.first()?.url.orEmpty()
            )
            Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).width(49.dp).height(49.dp)) {
                Image(
                    painter,
                    selectedTrack.track?.album?.images?.first()?.url.orEmpty(),
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)).width(49.dp).height(49.dp),
                    contentScale = ContentScale.Crop
                )
                if (isLoading.value) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).padding(8.dp),
                            color = Color(0xFFFACD66),
                        )
                    }
                }
            }
            Column(Modifier.weight(1f).padding(start = 8.dp).align(Alignment.Top)) {
                Text(
                    text = selectedTrack.track?.name.orEmpty(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = Color(
                            0XFFEFEEE0
                        )
                    )
                )
                Text(
                    text = selectedTrack.track?.artists?.map { it.name }?.joinToString(",").orEmpty(),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Row(modifier = Modifier.align(Alignment.CenterVertically)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp).size(32.dp).align(Alignment.CenterVertically)
                        .clickable(onClick = {
                            if (selectedIndex.value - 1 >= 0) {
                                selectedIndex.value -= 1
                            }
                        })
                )
                PlayPauseButton(
                    modifier = Modifier.padding(end = 8.dp).size(32.dp).align(Alignment.CenterVertically),
                    mediaPlayerController = mediaPlayerController,
                    selectedIndex = selectedIndex.value
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp).size(32.dp).align(Alignment.CenterVertically)
                        .clickable(onClick = {
                            if (selectedIndex.value < trackList.size - 1) {
                                selectedIndex.value += 1
                            }
                        })
                )
            }
        }
    }
}

@Composable
fun PlayPauseButton(modifier: Modifier, mediaPlayerController: MediaPlayerController, selectedIndex: Int) {
    val isPlaying = rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(selectedIndex) {
        isPlaying.value = true
    }
    if (isPlaying.value) Icon(
        Icons.Default.PlayArrow,
        contentDescription = null,
        modifier = modifier.clickable(onClick = {
            mediaPlayerController.pause()
            isPlaying.value = false
        })
    ) else Icon(
        imageVector = Icons.Default.Pause,
        contentDescription = null,
        modifier = modifier.clickable(onClick = {
            mediaPlayerController.start()
            isPlaying.value = true
        })
    )
}

private fun playTrack(
    selectedTrack: Item,
    mediaPlayerController: MediaPlayerController,
    isLoading: MutableState<Boolean>,
    selectedIndex: MutableState<Int>,
    trackList: List<Item>,
) {
    selectedTrack.track?.previewUrl?.let {
        mediaPlayerController.prepare(it, listener = object : MediaPlayerListener {
            override fun onReady() {
                isLoading.value = false
            }

            override fun onAudioCompleted() {
                if (selectedIndex.value < trackList.size - 1) {
                    selectedIndex.value += 1
                }
            }

            override fun onError() {
                if (selectedIndex.value < trackList.size - 1) {
                    selectedIndex.value += 1
                }
            }
        })
    } ?: run {
        if (selectedIndex.value < trackList.size - 1) {
            selectedIndex.value += 1
        } else {
            // selectedIndex.value = 0
        }
    }
}

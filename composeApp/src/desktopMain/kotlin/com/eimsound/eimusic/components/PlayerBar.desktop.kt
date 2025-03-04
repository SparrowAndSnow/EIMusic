package com.eimsound.eimusic.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.media.MediaPlayerController
import com.eimsound.eimusic.media.MediaPlayerListener
import com.eimsound.eimusic.network.SpotifyApiImpl
import com.eimsound.eimusic.network.models.topfiftycharts.Item

@Composable
actual fun PlayerBar(mediaPlayerController: MediaPlayerController) {
    var trackList by remember { mutableStateOf<List<Item>>(emptyList()) }
    val selectedIndex = remember { mutableStateOf(0) }
    val isLoading = remember { mutableStateOf(true) }
    val selectedTrack = if (trackList.isEmpty()) null else trackList[selectedIndex.value]
    var position = rememberSaveable { mutableStateOf(0.0f) }

    DisposableEffect(selectedTrack) {
        playTrack(selectedTrack, mediaPlayerController, isLoading, selectedIndex, position, trackList)
        onDispose {
            mediaPlayerController.release()
        }
    }

    LaunchedEffect(trackList) {
        trackList = SpotifyApiImpl().getTopFiftyChart().tracks?.items.orEmpty()
    }

    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp).clickable(
                indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val painter = rememberAsyncImagePainter(
                selectedTrack?.track?.album?.images?.first()?.url.orEmpty()
            )
            Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).width(48.dp).height(48.dp)) {
                Image(
                    painter,
                    selectedTrack?.track?.album?.images?.first()?.url.orEmpty(),
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)).width(48.dp).height(48.dp),
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
            Column(Modifier.fillMaxWidth(0.3f).padding(start = 8.dp).align(Alignment.Top)) {
                Text(
                    text = selectedTrack?.track?.name.orEmpty(),
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = selectedTrack?.track?.artists?.map { it.name }?.joinToString(",").orEmpty(),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Column(Modifier.padding(start = 8.dp).align(Alignment.CenterVertically)) {
                Slider(
                    value = position.value, onValueChange = { value ->
                        position.value = value
                        mediaPlayerController.duration?.let {
                            mediaPlayerController.seek(it.percentOf(value))
                        }
                    })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(32.dp)
                            .clickable(onClick = {
                                previousTrack(selectedIndex)
                            })
                    )
                    PlayPauseButton(
                        modifier = Modifier.padding(end = 8.dp).size(32.dp),
                        mediaPlayerController = mediaPlayerController,
                        selectedIndex = selectedIndex.value
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(32.dp)
                            .clickable(onClick = {
                                nextTrack(selectedIndex, trackList)
                            })
                    )
                }
            }
        }
    }
}

@Composable
fun PlayPauseButton(modifier: Modifier, mediaPlayerController: MediaPlayerController, selectedIndex: Int) {
    val isPlaying = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(selectedIndex) {
        isPlaying.value = true
    }
    if (isPlaying.value) Icon(
        Icons.Default.Pause,
        contentDescription = null,
        modifier = modifier.clickable(onClick = {
            mediaPlayerController.pause()
            isPlaying.value = false
        })
    ) else Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = null,
        modifier = modifier.clickable(onClick = {
            mediaPlayerController.start()
            isPlaying.value = true
        })
    )
}
private fun nextTrack(selectedIndex: MutableState<Int>, trackList: List<Item>) {
    if (selectedIndex.value < trackList.size - 1) {
        selectedIndex.value += 1
    }
}
private fun previousTrack(selectedIndex: MutableState<Int>) {
    if (selectedIndex.value - 1 >= 0) {
        selectedIndex.value -= 1
    }
}

private fun playTrack(
    selectedTrack: Item?,
    mediaPlayerController: MediaPlayerController,
    isLoading: MutableState<Boolean>,
    selectedIndex: MutableState<Int>,
    position: MutableState<Float>,
    trackList: List<Item>,
) = selectedTrack?.track?.previewUrl?.let {
    mediaPlayerController.prepare(it, listener = object : MediaPlayerListener {
        override fun onReady() {
            isLoading.value = false
            mediaPlayerController.start()
        }

        override fun onAudioCompleted() {
            nextTrack(selectedIndex, trackList)
        }

        override fun onError() {
            nextTrack(selectedIndex, trackList)
        }

        override fun timer(duration: Duration) {
            position.value = mediaPlayerController.duration?.parsePercent(duration.seconds) ?: 0f
        }
    })
} ?: run {
    nextTrack(selectedIndex, trackList)
}

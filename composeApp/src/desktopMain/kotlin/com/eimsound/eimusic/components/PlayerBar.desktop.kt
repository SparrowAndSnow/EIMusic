package com.eimsound.eimusic.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
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
import com.eimsound.eimusic.media.PlayMode
import com.eimsound.eimusic.network.SpotifyApiImpl
import com.eimsound.eimusic.network.models.topfiftycharts.Item

@Composable
actual fun PlayerBar(mediaPlayerController: MediaPlayerController) {
    var trackList by remember { mutableStateOf<List<Item>>(emptyList()) }
    val selectedIndex = remember { mutableStateOf(0) }
    val isLoading = remember { mutableStateOf(true) }
    val selectedTrack = if (trackList.isEmpty()) null else trackList[selectedIndex.value]
    var position = rememberSaveable { mutableStateOf(0.0f) }
    var playMode = remember { mutableStateOf(PlayMode.LOOP) }

    DisposableEffect(selectedTrack) {
        playTrack(
            selectedTrack,
            mediaPlayerController,
            isLoading,
            selectedIndex,
            position,
            trackList,
            playMode
        )
        onDispose {
            mediaPlayerController.release()
        }
    }

    LaunchedEffect(trackList) {
        trackList = SpotifyApiImpl().getTopFiftyChart().tracks?.items.orEmpty()
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackImage(selectedTrack = selectedTrack, isLoading = isLoading)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = selectedTrack?.track?.name.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                )
                Text(
                    text = selectedTrack?.track?.artists?.map { it.name }?.joinToString(",").orEmpty(),
                    modifier = Modifier.padding(top = 8.dp)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                )
            }
            PlayerControl(
                modifier = Modifier.padding(horizontal = 8.dp).weight(2f),
                mediaPlayerController,
                selectedIndex,
                trackList,
                position,
                playMode
            )
            Volume(modifier = Modifier.weight(1f).padding(start = 8.dp), mediaPlayerController)

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.Volume(modifier: Modifier = Modifier, mediaPlayerController: MediaPlayerController) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var isMute by rememberSaveable { mutableStateOf(mediaPlayerController.volume == 0.0) }
        var volume by rememberSaveable { mutableStateOf(mediaPlayerController.volume) }
        if (isMute) IconButton(
            onClick = {
                mediaPlayerController.volume = volume
                isMute = false
            }) {
            Icon(
                Icons.AutoMirrored.Default.VolumeOff,
                contentDescription = null
            )
        } else IconButton(onClick = {
            volume = mediaPlayerController.volume
            mediaPlayerController.volume = 0.0
            isMute = true
        }) {
            Icon(
                imageVector = if (volume > 0.8) {
                    Icons.AutoMirrored.Default.VolumeUp
                } else if (volume > 0.4) {
                    Icons.AutoMirrored.Default.VolumeDown
                } else {
                    Icons.AutoMirrored.Default.VolumeMute
                },
                contentDescription = null,
            )
        }
        Slider(value = volume.toFloat(), onValueChange = {
            volume = it.toDouble()
            mediaPlayerController.volume = it.toDouble()
            isMute = it.toDouble() == 0.0
        }, modifier = Modifier.height(16.dp).width(64.dp), track = { sliderState ->
            SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(8.dp))
        })
    }
}

@Composable
fun PlayPauseButton(modifier: Modifier = Modifier, mediaPlayerController: MediaPlayerController, selectedIndex: Int) {
    val isPlaying = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(selectedIndex) {
        isPlaying.value = true
    }
    if (isPlaying.value) IconButton(onClick = {
        mediaPlayerController.pause()
        isPlaying.value = false
    }) {
        Icon(
            modifier = modifier,
            imageVector = Icons.Default.PauseCircle,
            contentDescription = null
        )
    } else IconButton(onClick = {
        mediaPlayerController.start()
        isPlaying.value = true
    }) {
        Icon(
            modifier = modifier,
            imageVector = Icons.Default.PlayCircle,
            contentDescription = null
        )
    }
}

@Composable
fun RowScope.TrackImage(modifier: Modifier = Modifier, selectedTrack: Item?, isLoading: MutableState<Boolean>) {
    val painter = rememberAsyncImagePainter(
        selectedTrack?.track?.album?.images?.first()?.url.orEmpty()
    )
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp)).width(64.dp).height(64.dp)) {
        Image(
            painter,
            selectedTrack?.track?.album?.images?.first()?.url.orEmpty(),
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).width(64.dp).height(64.dp),
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
}

@Composable
fun RowScope.PlayerControl(
    modifier: Modifier = Modifier,
    mediaPlayerController: MediaPlayerController,
    selectedIndex: MutableState<Int>,
    trackList: List<Item>,
    position: MutableState<Float>,
    playMode: MutableState<PlayMode>
) {
    Column(modifier.align(Alignment.CenterVertically)) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                PlayMode(playMode)
                IconButton(modifier = Modifier.padding(horizontal = 4.dp).size(32.dp), onClick = {
                    previousTrack(selectedIndex)
                }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = null,
                    )
                }
                PlayPauseButton(
                    modifier = Modifier.size(48.dp).padding(horizontal = 4.dp),
                    mediaPlayerController = mediaPlayerController,
                    selectedIndex = selectedIndex.value
                )
                IconButton(modifier = Modifier.padding(horizontal = 4.dp).size(32.dp), onClick = {
                    nextTrack(selectedIndex, trackList)
                }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                    )
                }
            }
//            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.align(Alignment.BottomEnd)) {
                Text(
                    text = String.format(
                        "%02d:%02d",
                        mediaPlayerController.currentPosition?.minutesPart,
                        mediaPlayerController.currentPosition?.secondsPart
                    ),
                    style = MaterialTheme.typography.labelMedium,

                    )
                Text(text = " / ", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = String.format(
                        "%02d:%02d",
                        mediaPlayerController.duration?.minutesPart,
                        mediaPlayerController.duration?.secondsPart
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Slider(value = position.value, onValueChange = { value ->
            position.value = value
            mediaPlayerController.duration?.let {
                mediaPlayerController.seek(it.percentOf(value))
            }
        }, modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PlayMode(playMode: MutableState<PlayMode>) {
    IconButton(modifier = Modifier.padding(horizontal = 4.dp).size(32.dp), onClick = {
        playMode.value = playMode.value.change()
    }) {
        Icon(
            imageVector =
                when (playMode.value) {
                    PlayMode.LOOP -> Icons.Default.Repeat
                    PlayMode.REPEAT_ONE -> Icons.Default.RepeatOne
                    PlayMode.SHUFFLE -> Icons.Default.Shuffle
                },
            contentDescription = null,
        )
    }
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
    playMode: MutableState<PlayMode>
) = selectedTrack?.track?.previewUrl?.let {
    if (playMode.value == PlayMode.SHUFFLE) {
        selectedIndex.value = trackList.indices.random()
    }

    isLoading.value = true
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

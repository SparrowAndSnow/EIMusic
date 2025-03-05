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
    val trackList = remember { mutableStateOf<List<Item>>(emptyList()) }
    val shuffleList = remember { mutableStateOf<MutableSet<Item>>(mutableSetOf()) }
    val selectedIndex = remember { mutableStateOf(0) }
    val isLoading = remember { mutableStateOf(false) }
    val position = rememberSaveable { mutableStateOf(0.0f) }
    val playMode = remember { mutableStateOf(PlayMode.LOOP) }
    val volume = rememberSaveable { mutableStateOf(mediaPlayerController.volume) }
    val isMute = rememberSaveable { mutableStateOf(mediaPlayerController.isMuted) }
    val isPlaying = rememberSaveable { mutableStateOf(mediaPlayerController.isPlaying) }
    val selectedTrack = remember { mutableStateOf(trackList.value.getOrNull(selectedIndex.value)) }

    DisposableEffect(selectedTrack.value) {
        playTrack(
            selectedTrack,
            mediaPlayerController,
            isLoading,
            selectedIndex,
            position,
            trackList,
            shuffleList,
            playMode,
            volume,
            isMute,
            isPlaying
        )
        onDispose {
            mediaPlayerController.release()
        }
    }

    LaunchedEffect(playMode.value) {
        if (playMode.value == PlayMode.SHUFFLE && shuffleList.value.isEmpty()) {
            shuffleList.value.addAll(trackList.value.shuffled().toMutableSet())
        }
    }

    LaunchedEffect(trackList.value) {
        trackList.value = SpotifyApiImpl().getTopFiftyChart().tracks?.items.orEmpty()
        selectedTrack.value = trackList.value.getOrNull(selectedIndex.value)
    }

    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp).fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center), verticalAlignment = Alignment.CenterVertically
        ) {
            TrackImage(selectedTrack = selectedTrack.value, isLoading = isLoading)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = selectedTrack.value?.track?.name.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                )
                Text(
                    text = selectedTrack.value?.track?.artists?.map { it.name }?.joinToString(",").orEmpty(),
                    modifier = Modifier.padding(top = 8.dp)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                )
            }
            PlayerControl(
                modifier = Modifier.padding(horizontal = 8.dp).weight(2f),
                mediaPlayerController,
                selectedIndex,
                selectedTrack,
                trackList,
                shuffleList,
                position,
                playMode,
                isPlaying
            )
            Volume(volume, isMute, modifier = Modifier.weight(1f).padding(start = 8.dp), mediaPlayerController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.Volume(
    volume: MutableState<Double>,
    isMute: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    mediaPlayerController: MediaPlayerController
) {

    LaunchedEffect(volume.value) { mediaPlayerController.volume = volume.value }
    LaunchedEffect(isMute.value) { mediaPlayerController.isMuted = isMute.value }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        if (isMute.value) IconButton(
            onClick = {
                isMute.value = false
            }) {
            Icon(
                Icons.AutoMirrored.Default.VolumeOff, contentDescription = null
            )
        } else IconButton(onClick = {
            isMute.value = true
        }) {
            Icon(
                imageVector = if (volume.value > 0.8) {
                    Icons.AutoMirrored.Default.VolumeUp
                } else if (volume.value > 0.4) {
                    Icons.AutoMirrored.Default.VolumeDown
                } else {
                    Icons.AutoMirrored.Default.VolumeMute
                },
                contentDescription = null,
            )
        }
        Slider(value = volume.value.toFloat(), onValueChange = {
            volume.value = it.toDouble()
            isMute.value = it.toDouble() == 0.0
        }, modifier = Modifier.height(16.dp).width(64.dp), track = { sliderState ->
            SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(8.dp))
        })
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    mediaPlayerController: MediaPlayerController,
) {
    if (isPlaying.value) IconButton(onClick = {
        mediaPlayerController.pause()
        isPlaying.value = false
    }) {
        Icon(
            modifier = modifier, imageVector = Icons.Default.PauseCircle, contentDescription = null
        )
    } else IconButton(onClick = {
        mediaPlayerController.start()
        isPlaying.value = true
    }) {
        Icon(
            modifier = modifier, imageVector = Icons.Default.PlayCircle, contentDescription = null
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
    selectedTrack: MutableState<Item?>,
    trackList: State<List<Item>>,
    shuffleList: MutableState<MutableSet<Item>>,
    position: MutableState<Float>,
    playMode: MutableState<PlayMode>,
    isPlaying: MutableState<Boolean>
) {
    Column(modifier.align(Alignment.CenterVertically)) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                PlayMode(playMode)
                IconButton(modifier = Modifier.padding(horizontal = 4.dp).size(32.dp), onClick = {
                    previousTrack(selectedIndex, selectedTrack, playMode, trackList, shuffleList)
                }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = null,
                    )
                }
                PlayPauseButton(
                    isPlaying = isPlaying,
                    modifier = Modifier.size(48.dp).padding(horizontal = 4.dp),
                    mediaPlayerController = mediaPlayerController,
                )
                IconButton(modifier = Modifier.padding(horizontal = 4.dp).size(32.dp), onClick = {
                    nextTrack(selectedIndex, selectedTrack, playMode, trackList, shuffleList)
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
//            position.value = value
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
            imageVector = when (playMode.value) {
                PlayMode.LOOP -> Icons.Default.Repeat
                PlayMode.REPEAT_ONE -> Icons.Default.RepeatOne
                PlayMode.SHUFFLE -> Icons.Default.Shuffle
            },
            contentDescription = null,
        )
    }
}

private fun nextTrack(
    selectedIndex: MutableState<Int>,
    selectedTrack: MutableState<Item?>,
    playMode: State<PlayMode>,
    trackList: State<List<Item>>,
    shuffleList: State<MutableSet<Item>>
) {
    val list = if (playMode.value == PlayMode.SHUFFLE) shuffleList.value else trackList.value
    if (selectedIndex.value < list.size - 1) {
        selectedIndex.value += 1
        selectedTrack.value = list.toList()[selectedIndex.value]
    }
}

private fun previousTrack(
    selectedIndex: MutableState<Int>,
    selectedTrack: MutableState<Item?>,
    playMode: State<PlayMode>,
    trackList: State<List<Item>>,
    shuffleList: State<MutableSet<Item>>
) {
    val list = if (playMode.value == PlayMode.SHUFFLE) shuffleList.value else trackList.value
    if (selectedIndex.value - 1 >= 0) {
        selectedIndex.value -= 1
        selectedTrack.value = list.toList()[selectedIndex.value]
    }
}

private fun playTrack(
    selectedTrack: MutableState<Item?>,
    mediaPlayerController: MediaPlayerController,
    isLoading: MutableState<Boolean>,
    selectedIndex: MutableState<Int>,
    position: MutableState<Float>,
    trackList: State<List<Item>>,
    shuffleList: MutableState<MutableSet<Item>>,
    playMode: MutableState<PlayMode>,
    volume: State<Double>,
    isMute: State<Boolean>,
    isPlaying: MutableState<Boolean>
) = selectedTrack.value?.track?.previewUrl?.let {
    isLoading.value = true
    mediaPlayerController.prepare(it, listener = object : MediaPlayerListener {
        override fun onReady() {
            isLoading.value = false
            mediaPlayerController.volume = volume.value
            mediaPlayerController.isMuted = isMute.value
            mediaPlayerController.start()
            isPlaying.value = true
        }

        override fun onAudioCompleted() {
            when (playMode.value) {
                PlayMode.LOOP -> nextTrack(selectedIndex, selectedTrack, playMode, trackList, shuffleList)
                PlayMode.REPEAT_ONE -> {
                    mediaPlayerController.seek(Duration(0))
                    mediaPlayerController.start()
                }

                PlayMode.SHUFFLE -> {
                    shuffleList.value.removeIf { shuffleList.value.contains(selectedTrack.value) }
                    nextTrack(selectedIndex, selectedTrack, playMode, trackList, shuffleList)
                }
            }

        }

        override fun onError() {
            nextTrack(selectedIndex, selectedTrack, playMode, trackList, shuffleList)
        }

        override fun timer(duration: Duration) {
            position.value = mediaPlayerController.duration?.parsePercent(duration.seconds) ?: 0f
        }
    })
} ?: run {
    nextTrack(selectedIndex, selectedTrack, playMode, trackList, shuffleList)
}

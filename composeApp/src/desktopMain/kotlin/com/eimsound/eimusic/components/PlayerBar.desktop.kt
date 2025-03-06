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
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun PlayerBar(mediaPlayerController: MediaPlayerController) {
    val playerViewModel = koinViewModel<PlayerViewModel>()
    val state = playerViewModel.state.collectAsState()
    val playingListViewModel = koinViewModel<PlayingListViewModel>()

//    val trackList = remember { mutableStateOf<List<Item>>(emptyList()) }
//    val shuffleList = remember { mutableStateOf<MutableSet<Item>>(mutableSetOf()) }
//    val selectedIndex = remember { mutableStateOf(0) }
//    val isLoading = remember { mutableStateOf(false) }
//    val position = rememberSaveable { mutableStateOf(0.0f) }
//    val playMode = remember { mutableStateOf(PlayMode.LOOP) }
//    val volume = rememberSaveable { mutableStateOf(mediaPlayerController.volume) }
//    val isMute = rememberSaveable { mutableStateOf(mediaPlayerController.isMuted) }
//    val isPlaying = rememberSaveable { mutableStateOf(mediaPlayerController.isPlaying) }
//    val selectedTrack = remember { mutableStateOf(trackList.value.getOrNull(selectedIndex.value)) }

    DisposableEffect(playingListViewModel.selectedTrack.value) {
        playTrack(playingListViewModel, playerViewModel, mediaPlayerController)
        onDispose {
            mediaPlayerController.release()
        }
    }

    LaunchedEffect(state.value.playMode) {
        if (state.value.playMode == PlayMode.SHUFFLE && playingListViewModel.shuffleList.value.isEmpty()) {
            playingListViewModel.shuffleList.value.addAll(
                playingListViewModel.trackList.value.shuffled().toMutableSet()
            )
        }
    }

    LaunchedEffect(playingListViewModel.trackList.value) {
        val trackList = SpotifyApiImpl().getTopFiftyChart().tracks?.items.orEmpty()
        playingListViewModel.trackList.value = trackList
//        selectedTrack.value = trackList.value.getOrNull(selectedIndex.value)
    }

    LaunchedEffect(state.value.volume) { mediaPlayerController.volume = state.value.volume }
    LaunchedEffect(state.value.isMute) { mediaPlayerController.isMuted = state.value.isMute }

    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp).fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center), verticalAlignment = Alignment.CenterVertically
        ) {
            TrackImage(selectedTrack = playingListViewModel.selectedTrack.value, isLoading = state.value.isLoading)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = playingListViewModel.selectedTrack.value?.track?.name.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                )
                Text(
                    text = playingListViewModel.selectedTrack.value?.track?.artists?.map { it.name }?.joinToString(",")
                        .orEmpty(),
                    modifier = Modifier.padding(top = 8.dp)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                )
            }
            PlayerControl(
                modifier = Modifier.padding(horizontal = 8.dp).weight(2f),
                position = state.value.position,
                onPositionChanged = { position ->
                    mediaPlayerController.duration?.let {
                        playerViewModel.isLoading(true)
                        mediaPlayerController.seek(position, {
                            playerViewModel.isLoading(false)
                        })
                    }
                },
                duration = mediaPlayerController.duration ?: Duration(0),
                playMode = state.value.playMode,
                onPlayModeChanged = playerViewModel::onPlayModeChanged,
                isPlaying = state.value.isPlaying,
                onIsPlayingChanged = {
                    if (it) {
                        mediaPlayerController.start()
                        playerViewModel.isPlay(true)
                    } else {
                        mediaPlayerController.pause()
                        playerViewModel.isPlay(false)
                    }
                },
                onPreviousClick = { playingListViewModel.previous(state.value.playMode) },
                onNextClick = { playingListViewModel.next(state.value.playMode) }
            )
            Volume(
                state.value.volume,
                state.value.isMute,
                onVolumeChanged = playerViewModel::onVolumeChanged,
                onIsMuteChanged = playerViewModel::onIsMuteChanged,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.Volume(
    volume: Double,
    isMute: Boolean,
    onVolumeChanged: (Double) -> Unit = {},
    onIsMuteChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        if (isMute) IconButton(
            onClick = {
                onIsMuteChanged(false)
            }) {
            Icon(
                Icons.AutoMirrored.Default.VolumeOff, contentDescription = null
            )
        } else IconButton(onClick = {
            onIsMuteChanged(true)
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
            onIsMuteChanged(it.toDouble() == 0.0)
            onVolumeChanged(it.toDouble())
        }, modifier = Modifier.height(16.dp).width(64.dp), track = { sliderState ->
            SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(8.dp))
        })
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onIsPlayingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isPlaying) IconButton(onClick = {
        onIsPlayingChanged(false)
    }) {
        Icon(
            modifier = modifier, imageVector = Icons.Default.PauseCircle, contentDescription = null
        )
    } else IconButton(onClick = {
        onIsPlayingChanged(true)
    }) {
        Icon(
            modifier = modifier, imageVector = Icons.Default.PlayCircle, contentDescription = null
        )
    }
}

@Composable
fun RowScope.TrackImage(modifier: Modifier = Modifier, selectedTrack: Item?, isLoading: Boolean) {
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
        if (isLoading) {
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
    position: Duration,
    onPositionChanged: (Duration) -> Unit = {},
    playMode: PlayMode,
    onPlayModeChanged: (PlayMode) -> Unit = {},
    isPlaying: Boolean,
    onIsPlayingChanged: (Boolean) -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    duration: Duration
) {
    Column(modifier.align(Alignment.CenterVertically)) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                PlayModeButton(playMode, onPlayModeChanged)
                IconButton(modifier = Modifier.padding(horizontal = 4.dp).size(32.dp), onClick = onPreviousClick) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = null,
                    )
                }
                PlayPauseButton(
                    isPlaying = isPlaying,
                    onIsPlayingChanged = onIsPlayingChanged,
                    modifier = Modifier.size(48.dp).padding(horizontal = 4.dp),
                )
                IconButton(modifier = Modifier.padding(horizontal = 4.dp).size(32.dp), onClick = onNextClick) {
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
                        position?.minutesPart,
                        position?.secondsPart
                    ),
                    style = MaterialTheme.typography.labelMedium,

                    )
                Text(text = " / ", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = String.format(
                        "%02d:%02d",
                        duration?.minutesPart,
                        duration?.secondsPart
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        val progress = remember { mutableStateOf<Float?>(null) }
        Slider(value = progress.value ?: duration.toPercent(position), onValueChange = {
            progress.value = it
        }, onValueChangeFinished = {
            progress.value?.let {
                onPositionChanged(duration.percentOf(it))
                progress.value = null
            }
        }, modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PlayModeButton(playMode: PlayMode, onPlayModelChange: (playMode: PlayMode) -> Unit) {
    IconButton(modifier = Modifier.padding(horizontal = 4.dp).size(32.dp), onClick = {
        onPlayModelChange(playMode.change())
    }) {
        Icon(
            imageVector = when (playMode) {
                PlayMode.LOOP -> Icons.Default.Repeat
                PlayMode.REPEAT_ONE -> Icons.Default.RepeatOne
                PlayMode.SHUFFLE -> Icons.Default.Shuffle
            },
            contentDescription = null,
        )
    }
}

private fun playTrack(
    playingListViewModel: PlayingListViewModel,
    playerViewModel: PlayerViewModel,
    mediaPlayerController: MediaPlayerController
) {
    playingListViewModel.selectedTrack.value?.track?.previewUrl?.let {
        playerViewModel.isLoading(true)
        mediaPlayerController.prepare(it, listener = object : MediaPlayerListener {
            override fun onReady() {
                playerViewModel.isLoading(false)
                mediaPlayerController.volume = playerViewModel.state.value.volume
                mediaPlayerController.isMuted = playerViewModel.state.value.isMute
                mediaPlayerController.start()
                playerViewModel.isPlay(true)
            }

            override fun onAudioCompleted() {
                when (playerViewModel.state.value.playMode) {
                    PlayMode.LOOP -> playingListViewModel.next(playerViewModel.state.value.playMode)
                    PlayMode.REPEAT_ONE -> {
                        mediaPlayerController.seek(Duration(0))
                        mediaPlayerController.start()
                    }

                    PlayMode.SHUFFLE -> {
//                        vm.state.value.shuffleList.removeIf { shuffleList.value.contains(selectedTrack.value) }
                        playingListViewModel.next(playerViewModel.state.value.playMode)
                    }
                }

            }

            override fun onError() {
                playingListViewModel.next(playerViewModel.state.value.playMode)
            }

            override fun timer(duration: Duration) {
                playerViewModel.seek(duration)
            }

            override fun onLoading() {
                playerViewModel.isLoading(true)
            }

            override fun onLoaded() {
                playerViewModel.isLoading(false)
            }
        })
    } ?: run {
        playingListViewModel.next(playerViewModel.state.value.playMode)
    }
}


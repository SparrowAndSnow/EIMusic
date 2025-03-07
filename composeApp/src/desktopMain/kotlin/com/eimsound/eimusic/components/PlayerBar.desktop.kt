package com.eimsound.eimusic.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    val playingListViewModel = koinViewModel<PlayingListViewModel>()

    DisposableEffect(playingListViewModel.selectedTrack) {
        playTrack(playingListViewModel, playerViewModel, mediaPlayerController)
        onDispose {
            mediaPlayerController.release()
        }
    }

    LaunchedEffect(playerViewModel.playMode) {
        if (playerViewModel.playMode == PlayMode.SHUFFLE && playingListViewModel.shuffleList.isEmpty()) {
            playingListViewModel.shuffleList.addAll(
                playingListViewModel.trackList.shuffled().toMutableSet()
            )
        }
    }

    LaunchedEffect(playingListViewModel.trackList) {
        val trackList = SpotifyApiImpl().getTopFiftyChart().tracks?.items.orEmpty()
        playingListViewModel.load(trackList)
//        selectedTrack.value = trackList.value.getOrNull(selectedIndex.value)
    }

    LaunchedEffect(playerViewModel.volume) { mediaPlayerController.volume = playerViewModel.volume }
    LaunchedEffect(playerViewModel.isMute) { mediaPlayerController.isMuted = playerViewModel.isMute }

    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp).fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center), verticalAlignment = Alignment.CenterVertically
        ) {
            TrackImage(selectedTrack = playingListViewModel.selectedTrack, isLoading = playerViewModel.isLoading)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = playingListViewModel.selectedTrack?.track?.name.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                )
                Text(
                    text = playingListViewModel.selectedTrack?.track?.artists?.map { it.name }?.joinToString(",")
                        .orEmpty(),
                    modifier = Modifier.padding(top = 8.dp)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                )
            }
            PlayerControl(
                modifier = Modifier.padding(horizontal = 8.dp).weight(2f),
                position = playerViewModel.position,
                onPositionChanged = { position ->
                    mediaPlayerController.duration?.let {
                        playerViewModel.isLoading(true)
                        mediaPlayerController.seek(position, {
                            playerViewModel.isLoading(false)
                        })
                    }
                },
                duration = mediaPlayerController.duration ?: Duration(0),
                playMode = playerViewModel.playMode,
                onPlayModeChanged = playerViewModel::onPlayModeChanged,
                isPlaying = playerViewModel.isPlaying,
                onIsPlayingChanged = {
                    if (it) {
                        mediaPlayerController.start()
                        playerViewModel.isPlay(true)
                    } else {
                        mediaPlayerController.pause()
                        playerViewModel.isPlay(false)
                    }
                },
                onPreviousClick = { playingListViewModel.previous(playerViewModel.playMode) },
                onNextClick = { playingListViewModel.next(playerViewModel.playMode) },
            )
            Volume(
                playerViewModel.volume,
                playerViewModel.isMute,
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
    duration: Duration,
) {
    var daggingPosition by remember { mutableStateOf<Duration>(position) }
    var isDragging by remember { mutableStateOf(false) }

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
            TimeDisplay(
                modifier = Modifier.align(Alignment.BottomEnd), position, duration,
                isDragging,
                daggingPosition
            )
        }

        PlayerSlider(duration.toPercent(position), onValueChangeFinished = {
            isDragging = false
            onPositionChanged(duration.percentOf(it))
        }, onValueChange = {
            isDragging = true
            daggingPosition = duration.percentOf(it)
        })
    }
}

@Composable
fun TimeDisplay(
    modifier: Modifier = Modifier,
    position: Duration,
    duration: Duration,
    isDragging: Boolean,
    daggingPosition: Duration
) {
    Row(modifier = modifier) {
        Text(
            text = String.format(
                "%02d:%02d",
                if (isDragging) daggingPosition.minutesPart else position?.minutesPart,
                if (isDragging) daggingPosition.secondsPart else position?.secondsPart
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

@Composable
fun PlayerSlider(
    position: Float,
    onValueChangeFinished: (Float) -> Unit,
    onValueChange: (Float) -> Unit = {},
) {
    var progress by remember { mutableStateOf<Float>(position) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedPosition by animateFloatAsState(
        targetValue = if (isDragging) progress else position,
        animationSpec = tween(100)
    )
    LaunchedEffect(position) {
        if (!isDragging) progress = position
    }
    Slider(value = animatedPosition, onValueChange = {
        progress = it
        isDragging = true
        onValueChange(it)
    }, onValueChangeFinished = {
        progress?.let {
            onValueChangeFinished(it)
        }
        isDragging = false
    }, modifier = Modifier.height(32.dp))
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
    playingListViewModel.selectedTrack?.track?.previewUrl?.let {
        playerViewModel.isLoading(true)
        mediaPlayerController.prepare(it, listener = object : MediaPlayerListener {
            override fun onReady() {
                playerViewModel.isLoading(false)
                mediaPlayerController.volume = playerViewModel.volume
                mediaPlayerController.isMuted = playerViewModel.isMute
                mediaPlayerController.start()
                playerViewModel.isPlay(true)
            }

            override fun onAudioCompleted() {
                when (playerViewModel.playMode) {
                    PlayMode.LOOP -> playingListViewModel.next(playerViewModel.playMode)
                    PlayMode.REPEAT_ONE -> {
                        mediaPlayerController.seek(Duration(0))
                        mediaPlayerController.start()
                    }

                    PlayMode.SHUFFLE -> {
//                        vm.state.value.shuffleList.removeIf { shuffleList.value.contains(selectedTrack.value) }
                        playingListViewModel.next(playerViewModel.playMode)
                    }
                }

            }

            override fun onError() {
                playingListViewModel.next(playerViewModel.playMode)
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
        playingListViewModel.next(playerViewModel.playMode)
    }
}


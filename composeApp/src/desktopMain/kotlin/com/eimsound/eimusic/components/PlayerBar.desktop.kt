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
import com.eimsound.eimusic.layout.SidebarComponent
import com.eimsound.eimusic.media.PlayMode
import com.eimsound.eimusic.music.Album
import com.eimsound.eimusic.music.Artist
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.network.SpotifyApiImpl
import com.eimsound.eimusic.viewmodel.DefaultLayoutViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.eimsound.eimusic.viewmodel.PlayingListViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun PlayerBar() {
    val playerViewModel = koinViewModel<PlayerViewModel>()
    val playingListViewModel = koinViewModel<PlayingListViewModel>()
    val defaultLayoutViewModel = koinViewModel<DefaultLayoutViewModel>()

    val playerState by playerViewModel.state.collectAsState()
    val playingListState by playingListViewModel.state.collectAsState()
    val sideBarState by defaultLayoutViewModel.sideBarState.collectAsState()

    DisposableEffect(playingListState.selectedTrack) {
        playingListState.selectedTrack?.uri?.let {
            playerViewModel.play(it, playingListViewModel)
        } ?: run {
            playingListViewModel.next(playerState.playMode)
        }
        onDispose {
            playerViewModel.release()
        }
    }

//    LaunchedEffect(playerState.playMode) {
//        if (playerState.playMode == PlayMode.SHUFFLE && playingListState.shuffleList.isEmpty()) {
//            playingListViewModel.shuffleList.addAll(
//                playingListViewModel.trackList.shuffled().toMutableSet()
//            )
//        }
//    }

    LaunchedEffect(playingListState.trackList) {
        val trackList = SpotifyApiImpl().getTopFiftyChart().tracks?.items.orEmpty().map {
            val track = it.track
            val artists = track?.album?.artists?.map { it ->
                Artist(
                    name = it.name,
                    id = it.id,
                    image = it.uri
                )
            }
            Track(
                album = Album(
                    name = track?.album?.name,
                    image = track?.album?.images?.firstOrNull()?.url.orEmpty(),
                    releaseDate = track?.album?.releaseDate,
                    id = track?.album?.id,
                    totalTracks = track?.album?.totalTracks,
                    artists = artists
                ),
                artists = artists,
                name = track?.name,
                uri = track?.previewUrl,
                duration = Duration(track?.durationMs?.toLong()?.div(1000) ?: 0),
                id = track?.id,
                isLocal = track?.isLocal ?: false,
            )
        }
        playingListViewModel.load(trackList)
//        selectedTrack.value = trackList.value.getOrNull(selectedIndex.value)
    }


    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .padding(16.dp).fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center), verticalAlignment = Alignment.CenterVertically
        ) {
            TrackImage(selectedTrack = playingListState.selectedTrack, isLoading = playerState.isLoading)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = playingListState.selectedTrack?.name.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                )
                Text(
                    text = playingListState.selectedTrack?.artists?.joinToString(",") { it.name.toString() }
                        .orEmpty(),
                    modifier = Modifier.padding(top = 8.dp)
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                )
            }
            PlayerControl(
                modifier = Modifier.padding(horizontal = 8.dp).weight(2f),
                position = playerState.position,
                onPositionChanged = { position ->
                    playerState.duration?.let {
                        playerViewModel.isLoading(true)
                        playerViewModel.seek(position, {
                            playerViewModel.isLoading(false)
                        })
                    }
                },
                duration = playerState.duration ?: Duration(0),
                playMode = playerState.playMode,
                onPlayModeChanged = playerViewModel::onPlayModeChanged,
                isPlaying = playerState.isPlaying,
                onIsPlayingChanged = playerViewModel::isPlaying,
                onPreviousClick = { playingListViewModel.previous(playerState.playMode) },
                onNextClick = { playingListViewModel.next(playerState.playMode) },
            )
            Volume(
                playerState.volume,
                playerState.isMute,
                onVolumeChanged = playerViewModel::onVolumeChanged,
                onIsMuteChanged = playerViewModel::onIsMuteChanged,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            TrackListButton(showTrackList = sideBarState.showSideBar, onShowTrackListChanged = {
                defaultLayoutViewModel.updateSideBar(it, SidebarComponent.PLAYLIST)
            })
        }
    }
}

@Composable
fun TrackListButton(modifier: Modifier = Modifier, showTrackList: Boolean, onShowTrackListChanged: (Boolean) -> Unit) {
    IconButton(
        modifier = modifier,
        onClick = {
            onShowTrackListChanged(!showTrackList)
        }) {
        Icon(
            tint = if (showTrackList) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            imageVector = Icons.AutoMirrored.Default.QueueMusic, contentDescription = null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Volume(
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
fun TrackImage(modifier: Modifier = Modifier, selectedTrack: Track?, isLoading: Boolean) {
    val painter = rememberAsyncImagePainter(
        selectedTrack?.album?.image?.orEmpty()
    )
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp)).width(64.dp).height(64.dp)) {
        Image(
            painter,
            selectedTrack?.album?.image?.orEmpty(),
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).width(64.dp).height(64.dp),
            contentScale = ContentScale.Crop
        )
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).padding(8.dp),
                    color = MaterialTheme.colorScheme.primary,
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
    var draggingPosition by remember { mutableStateOf<Duration>(position) }
    var isDragging by remember { mutableStateOf(false) }

    Column(modifier.align(Alignment.CenterVertically)) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                PlayModeButton(playMode = playMode, onPlayModeChanged = onPlayModeChanged)
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
            TimeDisplay(
                modifier = Modifier.align(Alignment.BottomEnd), position, duration,
                isDragging,
                draggingPosition
            )
        }

        PlayerSlider(duration.toPercent(position), onValueChangeFinished = {
            isDragging = false
            onPositionChanged(duration.percentOf(it))
        }, onValueChange = {
            isDragging = true
            draggingPosition = duration.percentOf(it)
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
                duration.minutesPart,
                duration.secondsPart
            ),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun PlayerSlider(
    value: Float,
    onValueChangeFinished: (Float) -> Unit,
    onValueChange: (Float) -> Unit = {},
) {
    var progress by remember { mutableStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(if (isDragging) 0 else 200),
    )
    LaunchedEffect(value) {
        if (!isDragging) progress = value
    }
    Slider(value = animatedProgress, onValueChange = {
        isDragging = true
        progress = it
        onValueChange(it)
    }, onValueChangeFinished = {
        isDragging = false
        onValueChangeFinished(progress)
    }, modifier = Modifier.height(32.dp))
}

@Composable
fun PlayModeButton(
    modifier: Modifier = Modifier.padding(horizontal = 4.dp).size(32.dp),
    playMode: PlayMode,
    onPlayModeChanged: (playMode: PlayMode) -> Unit
) {
    IconButton(modifier = modifier, onClick = {
        onPlayModeChanged(playMode.change())
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


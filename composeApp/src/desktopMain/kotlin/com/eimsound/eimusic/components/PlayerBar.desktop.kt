package com.eimsound.eimusic.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.viewmodel.DefaultLayoutViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun PlayerBar() {
    val playerViewModel = koinViewModel<PlayerViewModel>()
    val defaultLayoutViewModel = koinViewModel<DefaultLayoutViewModel>()

    val playerState by playerViewModel.state.collectAsState()
    val sideBarState by defaultLayoutViewModel.sideBarState.collectAsState()

    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .padding(16.dp).fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center), verticalAlignment = Alignment.CenterVertically
        ) {
            TrackImage(selectedTrack = playerState.track, isLoading = playerState.isLoading, onClick = {
                defaultLayoutViewModel.updateFullScreenPlayer(true)
            })
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = playerState.track?.name.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                )
                Text(
                    text = playerState.track?.artists?.joinToString(",") { it.name.toString() }
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
                onPreviousClick = playerViewModel::previous,
                onNextClick = playerViewModel::next,
                bufferValue = playerState.bufferProgress,
                showBufferProgress = playerState.track != null
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



@Composable
fun RowScope.PlayerControl(
    modifier: Modifier = Modifier,
    position: Duration,
    bufferValue: Float,
    showBufferProgress: Boolean,
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
                PlayModeButton(modifier = Modifier.padding(horizontal = 4.dp).size(32.dp), playMode = playMode, onPlayModeChanged = onPlayModeChanged)
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

        PlayerSlider(
            value = duration.toPercent(position),
            showBufferProgress = showBufferProgress,
            bufferValue = bufferValue,
            onValueChangeFinished = {
                isDragging = false
                onPositionChanged(duration.percentOf(it))
            },
            onValueChange = {
                isDragging = true
                draggingPosition = duration.percentOf(it)
            }
        )
    }
}

@Composable
fun TrackImage(modifier: Modifier = Modifier, selectedTrack: Track?, isLoading: Boolean, onClick: () -> Unit) {
    val painter = rememberAsyncImagePainter(
        selectedTrack?.album?.image?.orEmpty()
    )
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp)).width(64.dp).height(64.dp).clickable(onClick = {
        onClick()
    })) {
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


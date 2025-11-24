package com.eimsound.eimusic.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.media.PlayMode

@Composable
fun TimeDisplay(
    modifier: Modifier = Modifier,
    position: Duration,
    duration: Duration,
    isDragging: Boolean,
    draggingPosition: Duration
) {
    Row(modifier = modifier) {
        Text(
            text = if (isDragging) {
                "${draggingPosition.minutesPart}:${draggingPosition.secondsPart}"
            } else {
                "${position.minutesPart}:${position.secondsPart}"
            },
            style = MaterialTheme.typography.labelMedium
        )
        Text(text = " / ", style = MaterialTheme.typography.labelMedium)
        Text(
            text = "${duration.minutesPart}:${duration.secondsPart}",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun PlayerSlider(
    value: Float,
    bufferValue: Float,
    showBufferProgress: Boolean,
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

    Column(modifier = Modifier.fillMaxWidth().height(36.dp)) {
        Slider(
            modifier = Modifier
                .fillMaxWidth().height(32.dp),
            value = animatedProgress,
            onValueChange = {
                isDragging = true
                progress = it
                onValueChange(it)
            },
            onValueChangeFinished = {
                onValueChangeFinished(progress)
                isDragging = false
            }
        )

        // 缓冲进度条
        AnimatedVisibility(
            visible = showBufferProgress && bufferValue < 1.0f,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val animatedProgress by animateFloatAsState(
                targetValue = bufferValue,
                animationSpec = tween(200),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

@Composable
fun PlayModeButton(
    modifier: Modifier = Modifier,
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
            contentDescription = when (playMode) {
                PlayMode.LOOP -> "列表循环"
                PlayMode.REPEAT_ONE -> "单曲循环"
                PlayMode.SHUFFLE -> "随机播放"
            }
        )
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
                contentDescription = null
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
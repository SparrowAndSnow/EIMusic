package com.eimsound.eimusic.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.util.pulsatingEffect
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.pause_circle_fill_24px
import eimusic.composeapp.generated.resources.play_circle_fill_24px
import eimusic.composeapp.generated.resources.repeat_24px
import eimusic.composeapp.generated.resources.repeat_one_24px
import eimusic.composeapp.generated.resources.shuffle_24px
import eimusic.composeapp.generated.resources.volume_down_fill_24px
import eimusic.composeapp.generated.resources.volume_mute_fill_24px
import eimusic.composeapp.generated.resources.volume_off_fill_24px
import eimusic.composeapp.generated.resources.volume_up_fill_24px
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock

@Composable
fun TimeDisplay(
    modifier: Modifier = Modifier,
    position: Duration,
    duration: Duration,
    isDragging: Boolean,
    draggingPosition: Duration
) {
    val palette = LocalPalette.current

    Row(modifier = modifier) {
        Text(
            text = if (isDragging) {
                draggingPosition.toString()
            } else {
                position.toString()
            },
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
        Text(text = " / ", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        Text(
            text = duration.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
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
    val palette = LocalPalette.current
    var progress by remember { mutableStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(if (isDragging) 0 else 180),
    )
    LaunchedEffect(value) {
        if (!isDragging) progress = value
    }

    Column(modifier = Modifier.fillMaxWidth().height(34.dp)) {
        Slider(
            modifier = Modifier.pulsatingEffect(
                animatedProgress,
                bufferValue,
                !isDragging && showBufferProgress && bufferValue < 1.0f
            )
                .fillMaxWidth()
                .height(20.dp),
            value = animatedProgress,
            onValueChange = {
                isDragging = true
                progress = it
                onValueChange(it)
            },
            onValueChangeFinished = {
                onValueChangeFinished(progress)
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        )

//        AnimatedVisibility(
//            visible = showBufferProgress && bufferValue < 1.0f,
//            enter = fadeIn() + expandVertically(),
//            exit = fadeOut() + shrinkVertically()
//        ) {
//            val animatedBuffer by animateFloatAsState(targetValue = bufferValue, animationSpec = tween(180))
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(3.dp)
//                    .clip(RoundedCornerShape(2.dp))
//                    .background(MaterialTheme.colorScheme.surfaceVariant)
//            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxHeight()
//                        .fillMaxWidth(animatedBuffer)
//                        .clip(RoundedCornerShape(2.dp))
//                        .background(palette.textSecondary)
//                )
//            }
//        }
    }
}

@Composable
fun PlayModeButton(
    modifier: Modifier = Modifier,
    playMode: PlayMode,
    onPlayModeChanged: () -> Unit
) {
    val palette = LocalPalette.current
    IconButton(modifier = modifier, onClick = {
        onPlayModeChanged()
    }) {
        Icon(
            painter = when (playMode) {
                is PlayMode.Loop -> painterResource(Res.drawable.repeat_24px)
                is PlayMode.RepeatOne -> painterResource(Res.drawable.repeat_one_24px)
                is PlayMode.Shuffle -> painterResource(Res.drawable.shuffle_24px)
            },
            tint = if (playMode == PlayMode.Loop) palette.textSecondary else palette.accent,
            contentDescription = when (playMode) {
                is PlayMode.Loop -> "列表循环"
                is PlayMode.RepeatOne -> "单曲循环"
                is PlayMode.Shuffle -> "随机播放"
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
    val palette = LocalPalette.current
    if (isPlaying) IconButton(onClick = { onIsPlayingChanged(false) }) {
        Icon(
            modifier = modifier,
            painter = painterResource(Res.drawable.pause_circle_fill_24px),
            tint = palette.accent,
            contentDescription = null
        )
    } else IconButton(onClick = { onIsPlayingChanged(true) }) {
        Icon(
            modifier = modifier,
            painter = painterResource(Res.drawable.play_circle_fill_24px),
            tint = palette.accent,
            contentDescription = null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Volume(
    volume: Float,
    isMute: Boolean,
    onVolumeChanged: (Float) -> Unit = {},
    onIsMuteChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalPalette.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isMute) IconButton(onClick = { onIsMuteChanged(false) }) {
            Icon(
                painterResource(Res.drawable.volume_off_fill_24px),
                contentDescription = null,
                tint = palette.textSecondary
            )
        } else IconButton(onClick = { onIsMuteChanged(true) }) {
            Icon(
                painter = if (volume > 0.8) {
                    painterResource(Res.drawable.volume_up_fill_24px)
                } else if (volume > 0.4) {
                    painterResource(Res.drawable.volume_down_fill_24px)
                } else {
                    painterResource(Res.drawable.volume_mute_fill_24px)
                },
                contentDescription = null,
                tint = palette.textSecondary
            )
        }
        Slider(
            value = volume,
            onValueChange = {
                onIsMuteChanged(it == 0.0F)
                onVolumeChanged(it)
            },
            modifier = Modifier
                .height(16.dp)
                .width(92.dp),
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp)
                )
            }
        )
    }
}

package com.eimsound.eimusic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.layout.SidebarComponent
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.theme.LocalRadii
import com.eimsound.eimusic.together.isPlaying
import com.eimsound.eimusic.viewmodel.DefaultLayoutViewModel
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.queue_music_24px
import eimusic.composeapp.generated.resources.skip_next_fill_24px
import eimusic.composeapp.generated.resources.skip_previous_fill_24px
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CommonPlayerBar(
    showVolume: Boolean = true,
    showTrackInfo: Boolean = true,
    showDuration: Boolean = true
) {
    val palette = LocalPalette.current
    val radii = LocalRadii.current
    val playerViewModel = koinViewModel<PlayerViewModel>()
    val defaultLayoutViewModel = koinViewModel<DefaultLayoutViewModel>()
    val state by playerViewModel.state.collectAsState()
    val sideBarState by defaultLayoutViewModel.sideBarState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = radii.medium,
        color = palette.surfaceGlass,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrackImage(
                modifier = Modifier.size(56.dp),
                image = state.current?.album?.image,
                isPlaying = state.playState.isPlaying,
                onPlayClick = { defaultLayoutViewModel.updateFullScreenPlayer(true) }
            )

            if (showTrackInfo) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.current?.name.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = state.current?.artists?.joinToString(",") { it.name ?: "" }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            PlayerMainControl(
                modifier = Modifier.weight(2f),
                position = Duration(state.position),
                onPositionChanged = { position ->
                    state.duration.let {
                        playerViewModel.sendSeek(position) { }
                    }
                },
                duration = Duration(state.duration),
                playMode = state.playMode,
                onPlayModeChanged = playerViewModel::sendPlayModeChanged,
                isPlaying = state.playState.isPlaying,
                onIsPlayingChanged = playerViewModel::sendIsPlaying,
                onPreviousClick = playerViewModel::sendPrevious,
                onNextClick = playerViewModel::sendNext,
                bufferValue = state.bufferProgress,
                showBufferProgress = state.playState.isPlaying,
                showDuration = showDuration
            )

            if (showVolume) {
                Volume(
                    volume = state.volume,
                    isMute = state.isMute,
                    onVolumeChanged = playerViewModel::onVolumeChanged,
                    onIsMuteChanged = playerViewModel::onIsMuteChanged,
                )
            }

            IconButton(
                onClick = {
                    defaultLayoutViewModel.updateSideBar(!sideBarState.showSideBar, SidebarComponent.PLAYLIST)
                }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.queue_music_24px),
                    contentDescription = "Queue",
                    tint = if (sideBarState.showSideBar) palette.accent else palette.textSecondary
                )
            }
        }
    }

    if (state.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(16.dp),
            color = palette.accent
        )
    }
}

@Composable
private fun RowScope.PlayerMainControl(
    modifier: Modifier = Modifier,
    position: Duration,
    bufferValue: Float,
    showBufferProgress: Boolean,
    showDuration: Boolean = true,
    onPositionChanged: (Duration) -> Unit = {},
    playMode: PlayMode,
    onPlayModeChanged: () -> Unit = {},
    isPlaying: Boolean,
    onIsPlayingChanged: (Boolean) -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    duration: Duration,
) {
    val palette = LocalPalette.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayModeButton(playMode = playMode, onPlayModeChanged = onPlayModeChanged)
            IconButton(onClick = onPreviousClick) {
                Icon(
                    painterResource(Res.drawable.skip_previous_fill_24px),
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }
            PlayPauseButton(
                isPlaying = isPlaying,
                onIsPlayingChanged = onIsPlayingChanged,
                modifier = Modifier.size(40.dp)
            )
            IconButton(onClick = onNextClick) {
                Icon(
                    painterResource(Res.drawable.skip_next_fill_24px),
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }
            if (showDuration) {
                TimeDisplay(
                    modifier = Modifier.padding(start = 6.dp),
                    position = position,
                    duration = duration,
                    isDragging = false,
                    draggingPosition = position
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0f))
                .clickable(enabled = false) {}
        ) {
            PlayerSlider(
                value = duration.toPercent(position),
                showBufferProgress = showBufferProgress && isPlaying,
                bufferValue = bufferValue,
                onValueChangeFinished = {
                    onPositionChanged(duration.percentOf(it))
                },
            )
        }
    }
}

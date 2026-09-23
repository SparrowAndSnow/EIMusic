package com.eimsound.eimusic.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.components.PlayModeButton
import com.eimsound.eimusic.components.PlayPauseButton
import com.eimsound.eimusic.components.PlayerSlider
import com.eimsound.eimusic.components.TimeDisplay
import com.eimsound.eimusic.components.TrackImage
import com.eimsound.eimusic.components.Volume
import com.eimsound.eimusic.components.WindowDragHandle
import com.eimsound.eimusic.music.Duration
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.together.isPlaying
import com.eimsound.eimusic.util.onBackHandler
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.keyboard_arrow_down_24px
import eimusic.composeapp.generated.resources.player_close
import eimusic.composeapp.generated.resources.player_next
import eimusic.composeapp.generated.resources.player_previous
import eimusic.composeapp.generated.resources.player_unknown_artist
import eimusic.composeapp.generated.resources.player_unknown_track
import eimusic.composeapp.generated.resources.skip_next_fill_24px
import eimusic.composeapp.generated.resources.skip_previous_fill_24px
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
expect fun FullScreenPlayer(onDismiss: () -> Unit)

@Composable
fun CommonFullScreenPlayer(
    onDismiss: () -> Unit,
    showVolume: Boolean = true
) {
    val palette = LocalPalette.current
    val playerViewModel = koinViewModel<PlayerViewModel>()
    val state by playerViewModel.state.collectAsState()

    onBackHandler(isBackEnabled = true) {
        onDismiss()
    }

    Surface(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface
    ) {
        WindowDragHandle(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TrackImage(
                        modifier = Modifier
                            .size(320.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        image = state.current?.album?.image,
                        isPlaying = state.playState.isPlaying,
                    ) {
                        playerViewModel.sendIsPlaying(!state.playState.isPlaying)
                    }

                    Text(
                        text = state.current?.name ?: stringResource(Res.string.player_unknown_track),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                    Text(
                        text = state.current?.artists?.joinToString(", ") { it.name ?: "" }
                            ?: stringResource(Res.string.player_unknown_artist),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    PlayerSlider(
                        value = Duration(state.duration).toPercent(Duration(state.position)),
                        bufferValue = state.bufferProgress,
                        showBufferProgress = state.playState.isPlaying,
                        onValueChangeFinished = { progress ->
                            state.duration.let { duration ->
                                playerViewModel.sendSeek(Duration(duration).percentOf(progress))
                            }
                        }
                    )

                    TimeDisplay(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        position = Duration(state.position),
                        duration = Duration(state.duration),
                        isDragging = false,
                        draggingPosition = Duration(0)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayModeButton(
                            playMode = state.playMode,
                            onPlayModeChanged = playerViewModel::sendPlayModeChanged
                        )
                        IconButton(onClick = playerViewModel::sendPrevious) {
                            Icon(
                                painter = painterResource(Res.drawable.skip_previous_fill_24px),
                                contentDescription = stringResource(Res.string.player_previous),
                                tint = palette.textSecondary
                            )
                        }
                        PlayPauseButton(
                            isPlaying = state.playState.isPlaying,
                            onIsPlayingChanged = playerViewModel::sendIsPlaying,
                            modifier = Modifier.size(72.dp)
                        )
                        IconButton(onClick = playerViewModel::sendNext) {
                            Icon(
                                painter = painterResource(Res.drawable.skip_next_fill_24px),
                                contentDescription = stringResource(Res.string.player_next),
                                tint = palette.textSecondary
                            )
                        }

                        if (showVolume) {
                            Volume(
                                volume = state.volume,
                                isMute = state.isMute,
                                onVolumeChanged = playerViewModel::onVolumeChanged,
                                onIsMuteChanged = playerViewModel::onIsMuteChanged
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.keyboard_arrow_down_24px),
                                contentDescription = stringResource(Res.string.player_close),
                                tint = palette.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

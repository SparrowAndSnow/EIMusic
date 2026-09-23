package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.dispatchQueueKey
import com.eimsound.eimusic.keyboard.queueStatusBarHint
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.ui.components.LocalStatusBarHint
import com.eimsound.eimusic.ui.components.TrackTable
import com.eimsound.eimusic.ui.theme.LocalSafeContentSize
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.jakewharton.mosaic.layout.*
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.ui.*

@Composable
fun QueueScreen(
    playerState: PlayerState,
    playerViewModel: PlayerViewModel,
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val safeArea = LocalSafeContentSize.current
    val termHeight = safeArea.rows
    val termWidth = safeArea.columns
    val visibleLines = (termHeight - 4).coerceAtLeast(1)
    var queueScroll by remember { mutableIntStateOf(0) }
    val statusBarHint = LocalStatusBarHint.current
    LaunchedEffect(Unit) {
        statusBarHint.value = queueStatusBarHint()
    }
    Box(
        modifier = Modifier.fillMaxSize().onKeyEvent { event ->
            dispatchQueueKey(
                event = event,
                onRemoveTrack = {
                    if (!playerState.queue.isEmpty() && queueScroll < playerState.queue.size){
                        playerViewModel.removeTrack(queueScroll)
                        queueScroll = (queueScroll - 1).coerceAtLeast(0)
                    }
                },
                onClearQueue = { playerViewModel.clearQueue() },
                onGoBack = onGoBack,
            )
        },
    ) {
        Column {
            Text(" Queue  (press 'o' to hide)", color = palette.titleFg)

            TrackTable(
                tracks = playerState.queue,
                currentTrackId = playerState.current?.id,
                termWidth = termWidth,
                visibleLines = visibleLines,
                onPlayTrack = { track -> playerViewModel.playTrack(track) },
                onSelectedChange = { queueScroll = it },
                onConfirm = {
                    playerViewModel.playTrack(it)
                    onGoBack()
                }
            )
        }
    }
}

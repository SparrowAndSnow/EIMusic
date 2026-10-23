package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.*
import com.eimsound.eimusic.keyboard.QueueKeyActions
import com.eimsound.eimusic.keyboard.dispatchQueueKey
import com.eimsound.eimusic.keyboard.queueStatusBarHint
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.ui.components.ScreenFrame
import com.eimsound.eimusic.ui.components.TrackTable
import com.eimsound.eimusic.ui.components.rememberListSelectionState
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
    val selection = rememberListSelectionState()
    ScreenFrame(
        hint = queueStatusBarHint(),
        onKey = { event ->
            dispatchQueueKey(
                event = event,
                actions = QueueKeyActions(
                    moveUp = {
                        val index = selection.index
                        if (index > 0 && index < playerState.queue.size) {
                            playerViewModel.moveTrack(index, index - 1)
                            selection.jumpTo(index - 1)
                        }
                    },
                    moveDown = {
                        val index = selection.index
                        if (index >= 0 && index < playerState.queue.size - 1) {
                            playerViewModel.moveTrack(index, index + 1)
                            selection.jumpTo(index + 1)
                        }
                    },
                    removeTrack = {
                        val index = selection.index
                        if (index >= 0 && index < playerState.queue.size) {
                            playerViewModel.removeTrack(index)
                            selection.jumpTo((index - 1).coerceAtLeast(0))
                        }
                    },
                    clearQueue = {
                        playerViewModel.clearQueue()
                        selection.jumpTo(0)
                    },
                    goBack = onGoBack,
                ),
            )
        },
    ) {
        Column {
            Text(" Queue", color = palette.titleFg)

            TrackTable(
                tracks = playerState.queue,
                currentTrackId = playerState.current?.id,
                termWidth = termWidth,
                visibleLines = visibleLines,
                onPlayTrack = { track -> playerViewModel.playTrack(track) },
                selection = selection,
                onConfirm = {
                    playerViewModel.playTrack(it)
                    onGoBack()
                }
            )
        }
    }
}

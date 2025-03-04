package com.eimsound.eimusic.layout

import androidx.compose.runtime.Composable
import com.eimsound.eimusic.components.PlayerBar
import com.eimsound.eimusic.media.MediaPlayerController

@Composable
fun BottomBar(mediaPlayerController: MediaPlayerController) {
    PlayerBar(mediaPlayerController)
}

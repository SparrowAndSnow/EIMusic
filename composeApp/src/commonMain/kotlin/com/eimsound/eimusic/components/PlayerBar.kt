package com.eimsound.eimusic.components


import androidx.compose.runtime.*
import com.eimsound.eimusic.media.MediaPlayerController


@Composable
expect fun PlayerBar(mediaPlayerController: MediaPlayerController)
package com.eimsound.eimusic.components

import androidx.compose.runtime.Composable

@Composable
actual fun PlayerBar() {
    CommonPlayerBar(showVolume = false, showTrackInfo = false, showDuration = false)
}

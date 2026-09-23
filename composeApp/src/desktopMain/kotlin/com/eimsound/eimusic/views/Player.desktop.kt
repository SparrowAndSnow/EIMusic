package com.eimsound.eimusic.views

import androidx.compose.runtime.Composable

@Composable
actual fun FullScreenPlayer(onDismiss: () -> Unit) {
    CommonFullScreenPlayer(onDismiss)
}
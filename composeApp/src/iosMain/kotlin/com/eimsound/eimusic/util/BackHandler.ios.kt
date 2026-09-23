package com.eimsound.eimusic.util

import androidx.compose.runtime.Composable

@Composable
actual fun onBackHandler(
    isBackEnabled: Boolean,
    onBackCancelled: () -> Unit,
    onBackCompleted: () -> Unit
) {
}
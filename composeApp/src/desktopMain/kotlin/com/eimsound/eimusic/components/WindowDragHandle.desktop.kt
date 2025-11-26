package com.eimsound.eimusic.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.window.WindowDraggableArea
import com.eimsound.eimusic.LocalWindowScope

@Composable
actual fun WindowDragHandle(modifier: Modifier) {
    val window = LocalWindowScope.current
    window?.WindowDraggableArea {
        Box(modifier)
    }
}
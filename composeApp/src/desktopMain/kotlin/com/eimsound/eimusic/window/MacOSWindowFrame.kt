package com.eimsound.eimusic.window

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import org.jetbrains.skiko.disableTitleBar

@Composable
fun FrameWindowScope.MacOSWindowFrame(
    state: WindowState,
    title: String,
    icon: Painter?,
    captionBarHeight: Dp,
    content: @Composable (windowInset: WindowInsets, captionBarInset: WindowInsets) -> Unit
) {
    val windowInset by remember(state) {
        derivedStateOf {
            if (state.placement != WindowPlacement.Fullscreen) {
                WindowInsets(top = captionBarHeight)
            } else {
                WindowInsets(0)
            }
        }
    }
    LaunchedEffect(window, captionBarHeight) {
        window.findSkiaLayer()?.disableTitleBar(captionBarHeight.value)
    }
    //TODO Get real macOS caption bar width.
    Box {
        val contentInset = WindowInsets(left = 80.dp)
        content(windowInset, contentInset)
        Row(modifier = Modifier.windowInsetsPadding(contentInset)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(captionBarHeight)
            ) {
                if (icon != null) {
                    Image(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 6.dp).size(16.dp)
                    )
                }
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

    }

    window.rootPane.apply {
        rootPane.putClientProperty("apple.awt.fullWindowContent", true)
        rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
        rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
    }
}
package com.eimsound.eimusic.window

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowState
import com.eimsound.eimusic.jna.windows.structure.isWindows10OrLater
import com.eimsound.eimusic.jna.windows.structure.isWindows11OrLater
import org.jetbrains.skiko.hostOs

@Composable
fun FrameWindowScope.WindowFrame(
    titleBar: @Composable () -> Unit = {},
    onCloseRequest: () -> Unit,
    icon: Painter? = null,
    title: String = "",
    state: WindowState,
    captionBarHeight: Dp = 48.dp,
    content: @Composable (windowInset: WindowInsets, captionBarInset: WindowInsets) -> Unit
) {
//    val supportBackdrop = hostOs.isWindows && isWindows11OrLater()
    when {
        hostOs.isWindows && isWindows10OrLater() -> {
            WindowsWindowFrame(
                titleBar = titleBar,
                onCloseRequest = onCloseRequest,
                icon = icon,
                title = title,
                content = content,
                state = state,
                captionBarHeight = captionBarHeight
            )
        }

        hostOs.isMacOS -> {
            MacOSWindowFrame(
                content = content,
                captionBarHeight = captionBarHeight,
                icon = icon,
                title = title,
                state = state
            )
        }

        else -> {
            content(WindowInsets(0), WindowInsets(0))
        }
    }
}
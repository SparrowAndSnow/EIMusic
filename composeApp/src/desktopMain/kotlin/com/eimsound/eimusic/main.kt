package com.eimsound.eimusic

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.eimsound.eimusic.window.WindowFrame
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.app_name
import javafx.application.Platform
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension

fun main() = application {
    val state = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(1280.dp, 720.dp)
    )
    Platform.startup {}
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
        state = state
    ) {
        window.minimumSize = Dimension(720, 480)
        App(
            windowFrame = {
                WindowFrame(
                    titleBar = {},
                    onCloseRequest = ::exitApplication,
                    icon = null,
                    title = stringResource(Res.string.app_name),
                    state = state,
                ) { windowInset, contentInset ->
                    it(windowInset, contentInset)
                }
            },
        )
    }
}
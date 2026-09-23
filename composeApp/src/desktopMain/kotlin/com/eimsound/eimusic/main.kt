package com.eimsound.eimusic

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.eimsound.eimusic.window.WindowFrame
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.app_name
import io.github.vinceglb.filekit.FileKit
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension
import kotlin.system.exitProcess

val LocalWindowScope = staticCompositionLocalOf<FrameWindowScope?> { null }

fun main() {
    FileKit.init(appId = "EIMusic")
    initKoin()

    application {
        val state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1280.dp, 720.dp)
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            state = state
        ) {
            CompositionLocalProvider(LocalWindowScope provides this) {
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
    }
}
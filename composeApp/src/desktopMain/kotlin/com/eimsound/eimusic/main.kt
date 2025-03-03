package com.eimsound.eimusic

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.app_name
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension

fun main() = application {
    val state = rememberWindowState()
//    var canDraggable = rememberSaveable { mutableStateOf(true) }
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
        state = state
    ) {
        window.minimumSize = Dimension(720, 480)

        DevelopmentEntryPoint {
//            Corner(state) {
//                Column {
//                    WindowTitleBar(state, canDraggable.value, "EIMusic", onClose = ::exitApplication, onMaximize = {
//                        state.placement = if (state.placement == WindowPlacement.Maximized) {
//                            window.isResizable = true
//                            canDraggable.value = true
//                            WindowPlacement.Floating
//                        } else {
//                            window.isResizable = false
//                            canDraggable.value = false
//                            WindowPlacement.Maximized
//                        }
//                    })
//                    App()
//                }
//            }
            App()

        }
    }
}



@Composable
private fun Corner(state: WindowState, content: @Composable () -> Unit) {
    if (state.placement == WindowPlacement.Floating) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        ) {
            content()
        }
    } else {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WindowScope.WindowTitleBar(
    state: WindowState,
    canDraggable: Boolean,
    title: String,
    onClose: () -> Unit,
    onMinimize: () -> Unit = { state.isMinimized = state.isMinimized.not() },
    onMaximize: () -> Unit = { },
) = Surface {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WindowTitleBar(
            canDraggable,
            modifier = Modifier.weight(1f)
                .padding(start = 20.dp)
                .fillMaxHeight()
                .combinedClickable(onClick = {}, onDoubleClick = onMaximize)
        ) {
            Text(text = title, color = Color.Black, modifier = Modifier.align(Alignment.CenterVertically))
        }

        Row {
            WindowControlButton(
                onClick = onMinimize
            )
            Spacer(modifier = Modifier.width(5.dp))
            WindowControlButton(
                onClick = onMaximize
            )
            Spacer(modifier = Modifier.width(5.dp))
            WindowControlButton(
                onClick = onClose
            )
        }
    }
}

@Composable
fun WindowControlButton(
    text: String = "",
    onClick: () -> Unit = {},
    color: Color = Color(210, 210, 210),
    size: Int = 16
) {
    val interactionSource = remember { MutableInteractionSource() }
    val buttonHover = interactionSource.collectIsHoveredAsState()

    Surface(
        color = if (buttonHover.value)
            Color(color.red / 1.3f, color.green / 1.3f, color.blue / 1.3f)
        else
            color,
        shape = RoundedCornerShape((size / 2).dp)
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = onClick)
                .size(size.dp, size.dp)
                .hoverable(interactionSource)
        ) {
            Text(text = text)
        }
    }
}

@Composable
fun WindowScope.WindowTitleBar(canDraggable: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    if (canDraggable) {
        WindowDraggableArea(modifier = modifier) {
            content()
        }
    } else {
        Row(modifier = modifier) {
            content()
        }
    }
}

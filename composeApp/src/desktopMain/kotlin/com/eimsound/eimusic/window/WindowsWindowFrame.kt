package com.eimsound.eimusic.window

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Maximize
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.FontLoadResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.zIndex
import com.eimsound.eimusic.jna.windows.ComposeWindowProcedure
import com.eimsound.eimusic.jna.windows.handleThemeChange
import com.eimsound.eimusic.jna.windows.structure.WinUserConst.HTCAPTION
import com.eimsound.eimusic.jna.windows.structure.WinUserConst.HTCLIENT
import com.eimsound.eimusic.jna.windows.structure.WinUserConst.HTCLOSE
import com.eimsound.eimusic.jna.windows.structure.WinUserConst.HTMAXBUTTON
import com.eimsound.eimusic.jna.windows.structure.WinUserConst.HTMINBUTTON
import com.eimsound.eimusic.theme.Theme
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import java.awt.Window

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrameWindowScope.WindowsWindowFrame(
    titleBar: @Composable () -> Unit = {},
    background: Color = MaterialTheme.colorScheme.background,
    onCloseRequest: () -> Unit,
    icon: Painter? = null,
    title: String = "",
    state: WindowState,
    captionBarHeight: Dp = 48.dp,
    content: @Composable (windowInset: WindowInsets, captionBarInset: WindowInsets) -> Unit
) {
    LaunchedEffect(window) {
        window.findSkiaLayer()?.transparency = true
    }

    val isDarkTheme = Theme.current
    LaunchedEffect(isDarkTheme) {
        handleThemeChange(window, isDarkTheme)
    }

    val paddingInset = remember { MutableWindowInsets() }
    val maxButtonRect = remember { mutableStateOf(Rect.Zero) }
    val minButtonRect = remember { mutableStateOf(Rect.Zero) }
    val closeButtonRect = remember { mutableStateOf(Rect.Zero) }
    val captionBarRect = remember { mutableStateOf(Rect.Zero) }
    val layoutHitTestOwner = rememberLayoutHitTestOwner()
    val contentPaddingInset = remember { MutableWindowInsets() }
    val procedure = remember(window) {
        ComposeWindowProcedure(
            window = window,
            hitTest = { x, y ->
                when {
                    maxButtonRect.value.contains(x, y) -> HTMAXBUTTON
                    minButtonRect.value.contains(x, y) -> HTMINBUTTON
                    closeButtonRect.value.contains(x, y) -> HTCLOSE
                    captionBarRect.value.contains(x, y) && !layoutHitTestOwner.hitTest(x, y) -> HTCAPTION

                    else -> HTCLIENT
                }
            },
            onWindowInsetUpdate = { paddingInset.insets = it }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(paddingInset)
            .background(background)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(WindowInsets(top = captionBarHeight), contentPaddingInset)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(captionBarHeight)
                .zIndex(10f)
                .onGloballyPositioned { captionBarRect.value = it.boundsInWindow() }
        ) {
            icon?.let {
                Image(
                    painter = it,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 6.dp).size(16.dp)
                )
            }
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            titleBar()
            Spacer(modifier = Modifier.weight(1f))
            window.CaptionButtonRow(
                windowHandle = procedure.windowHandle,
                isMaximize = state.placement == WindowPlacement.Maximized,
                onCloseRequest = onCloseRequest,
                onMaximizeButtonRectUpdate = {
                    maxButtonRect.value = it
                },
                onMinimizeButtonRectUpdate = {
                    minButtonRect.value = it
                },
                onCloseButtonRectUpdate = {
                    closeButtonRect.value = it
                },
                accentColor = procedure.windowFrameColor,
                frameColorEnabled = procedure.isWindowFrameAccentColorEnabled,
                isActive = procedure.isWindowActive,
                modifier = Modifier.align(Alignment.Top).onSizeChanged {
                    contentPaddingInset.insets = WindowInsets(right = it.width, top = it.height)
                }
            )
        }
    }
}

@Composable
fun Window.CaptionButtonRow(
    windowHandle: HWND,
    isMaximize: Boolean,
    isActive: Boolean,
    accentColor: Color,
    frameColorEnabled: Boolean,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onMaximizeButtonRectUpdate: (Rect) -> Unit,
    onMinimizeButtonRectUpdate: (Rect) -> Unit = {},
    onCloseButtonRectUpdate: (Rect) -> Unit = {}
) {
    //Draw the caption button
    Row(
        modifier = modifier
            .zIndex(1f)
    ) {
        val colors = if (frameColorEnabled && accentColor != Color.Unspecified) {
            CaptionButtonDefaults.accentColors(accentColor)
        } else {
            CaptionButtonDefaults.defaultColors()
        }
        CaptionButton(
            onClick = {
                User32.INSTANCE.ShowWindow(windowHandle, WinUser.SW_MINIMIZE)
            },
            icon = CaptionButtonIcon.Minimize,
            isActive = isActive,
            colors = colors,
            modifier = Modifier.onGloballyPositioned {
                onMinimizeButtonRectUpdate(it.boundsInWindow())
            }
        )
        CaptionButton(
            onClick = {
                if (isMaximize) {
                    User32.INSTANCE.ShowWindow(
                        windowHandle,
                        WinUser.SW_RESTORE
                    )
                } else {
                    User32.INSTANCE.ShowWindow(
                        windowHandle,
                        WinUser.SW_MAXIMIZE
                    )
                }
            },
            icon = if (isMaximize) {
                CaptionButtonIcon.Restore
            } else {
                CaptionButtonIcon.Maximize
            },
            isActive = isActive,
            colors = colors,
            modifier = Modifier.onGloballyPositioned {
                onMaximizeButtonRectUpdate(it.boundsInWindow())
            }
        )
        CaptionButton(
            icon = CaptionButtonIcon.Close,
            onClick = onCloseRequest,
            isActive = isActive,
            colors = CaptionButtonDefaults.closeColors(),
            modifier = Modifier.onGloballyPositioned {
                onCloseButtonRectUpdate(it.boundsInWindow())
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CaptionButton(
    onClick: () -> Unit,
    icon: CaptionButtonIcon,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    colors: VisualStateScheme<CaptionButtonColor> = CaptionButtonDefaults.defaultColors(),
    interaction: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val color = colors.schemeFor(interaction.collectVisualState(false))
    Box(
        modifier = modifier.size(46.dp, 32.dp)
            .background(if (isActive) color.background else color.inactiveBackground)
            .clickable(
                onClick = onClick,
                interactionSource = interaction,
                indication = null
            ),
    ) {
        val fontFamily by rememberFontIconFamily()
        val color = if (isActive) color.foreground else color.inactiveForeground
        if (fontFamily != null) {
            Text(
                color = color,
                text = icon.glyph.toString(),
                fontFamily = fontFamily,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
            )
        } else {
            Icon(
                tint = color,
                imageVector = icon.imageVector,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center).size(13.dp),
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberFontIconFamily(): State<FontFamily?> {
    val fontIconFamily = remember { mutableStateOf<FontFamily?>(null) }
    // Get windows system font icon, if get failed fall back to fluent svg icon.
    val fontFamilyResolver = LocalFontFamilyResolver.current
    LaunchedEffect(fontFamilyResolver) {
        fontIconFamily.value = sequenceOf("Segoe Fluent Icons", "Segoe MDL2 Assets")
            .mapNotNull {
                val fontFamily = FontFamily(it)
                runCatching {
                    val result = fontFamilyResolver.resolve(fontFamily).value as FontLoadResult
                    if (result.typeface == null || result.typeface?.familyName != it) {
                        null
                    } else {
                        fontFamily
                    }
                }.getOrNull()
            }
            .firstOrNull()
    }
    return fontIconFamily
}

object CaptionButtonDefaults {
    @Composable
    @Stable
    fun defaultColors(
        default: CaptionButtonColor = CaptionButtonColor(
            background = Color.Unspecified,
            foreground = MaterialTheme.colorScheme.primary,
            inactiveBackground = Color.Unspecified,
            inactiveForeground = MaterialTheme.colorScheme.primary
        ),
        hovered: CaptionButtonColor = default.copy(
            background = MaterialTheme.colorScheme.primary,
            foreground = Color.White,
            inactiveBackground = MaterialTheme.colorScheme.primary,
            inactiveForeground = Color.White,
        ),
        pressed: CaptionButtonColor = default.copy(
            background = MaterialTheme.colorScheme.primary.copy(0.9f),
            foreground = Color.White.copy(0.7f),
            inactiveBackground = MaterialTheme.colorScheme.primary.copy(0.9f),
            inactiveForeground = Color.White.copy(0.7f)
        ),
        disabled: CaptionButtonColor = default.copy(
            foreground = MaterialTheme.colorScheme.error
        ),
    ) = PentaVisualScheme(
        default = default,
        hovered = hovered,
        pressed = pressed,
        disabled = disabled
    )

    @Composable
    @Stable
    fun accentColors(
        accentColor: Color,
        default: CaptionButtonColor = CaptionButtonColor(
            background = Color.Unspecified,
            foreground = MaterialTheme.colorScheme.primary,
            inactiveBackground = Color.Unspecified,
            inactiveForeground = MaterialTheme.colorScheme.primary
        ),
        hovered: CaptionButtonColor = default.copy(
            background = accentColor,
            foreground = Color.White,
            inactiveBackground = accentColor,
            inactiveForeground = Color.White
        ),
        pressed: CaptionButtonColor = default.copy(
            background = accentColor.copy(0.9f),
            foreground = Color.White.copy(0.7f),
            inactiveBackground = accentColor.copy(0.9f),
            inactiveForeground = Color.White.copy(0.7f)
        ),
        disabled: CaptionButtonColor = default.copy(
            foreground = accentColor
        ),
    ) = PentaVisualScheme(
        default = default,
        hovered = hovered,
        pressed = pressed,
        disabled = disabled
    )

    @Composable
    @Stable
    fun closeColors() = accentColors(Color(0xFFC42B1C))
}

@Stable
data class CaptionButtonColor(
    val background: Color,
    val foreground: Color,
    val inactiveBackground: Color,
    val inactiveForeground: Color
)

enum class CaptionButtonIcon(
    val glyph: Char,
    val imageVector: ImageVector
) {
    Minimize(
        glyph = '\uE921',
        imageVector = Icons.Filled.Minimize
    ),
    Maximize(
        glyph = '\uE922',
        imageVector = Icons.Filled.Maximize
    ),
    Restore(
        glyph = '\uE923',
        imageVector = Icons.Filled.Restore
    ),
    Close(
        glyph = '\uE8BB',
        imageVector = Icons.Filled.Close
    )
}

fun Rect.contains(x: Float, y: Float): Boolean {
    return x >= left && x < right && y >= top && y < bottom
}

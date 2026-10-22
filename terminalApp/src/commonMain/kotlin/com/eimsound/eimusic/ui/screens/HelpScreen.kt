package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.eimsound.eimusic.keyboard.HelpAction
import com.eimsound.eimusic.keyboard.helpStatusBarHint
import com.eimsound.eimusic.keyboard.mainHelpEntries
import com.eimsound.eimusic.keyboard.mapHelpKey
import com.eimsound.eimusic.ui.components.Dialog
import com.eimsound.eimusic.ui.components.LocalStatusBarHint
import com.eimsound.eimusic.ui.LocalUiInputEnabled
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.ui.uiKeyEvent
import com.eimsound.eimusic.util.spacer
import com.jakewharton.mosaic.layout.fillMaxSize
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text

@Composable
fun HelpScreen(
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val statusBarHint = LocalStatusBarHint.current
    val uiActive = LocalUiInputEnabled.current
    LaunchedEffect(uiActive) {
        statusBarHint.value = helpStatusBarHint()
    }
    Box(Modifier.fillMaxSize().uiKeyEvent { event->
        if (mapHelpKey(event.key) == HelpAction.Dismiss) onGoBack()
        true
    }, contentAlignment = Alignment.Center) {
        Dialog(title = "Keyboard Controls", onDismiss = onGoBack) {
            Column {
                mainHelpEntries.forEach { (key, desc) ->
                    Text(
                        buildAnnotatedString {
                            spacer(4)
                            withStyle(SpanStyle(color = palette.highlightFg)) { append(key) }
                            spacer(4)
                            withStyle(SpanStyle(color = palette.textDimFg)) { append(desc) }
                        },
                        color = palette.textFg,
                    )
                }
            }
        }
    }
}
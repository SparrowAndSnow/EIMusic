package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.eimsound.eimusic.keyboard.helpStatusBarHint
import com.eimsound.eimusic.keyboard.mainHelpEntries
import com.eimsound.eimusic.ui.components.DialogPanel
import com.eimsound.eimusic.ui.components.LocalStatusBarHint
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.spacer
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Dialog as OverlayDialog
import com.jakewharton.mosaic.ui.Text

@Composable
fun HelpDialog(
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    val statusBarHint = LocalStatusBarHint.current

    LaunchedEffect(Unit) {
        statusBarHint.value = helpStatusBarHint()
    }

    OverlayDialog(onDismissRequest = onGoBack) {
        DialogPanel(title = "Keyboard Controls", onDismiss = onGoBack) {
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

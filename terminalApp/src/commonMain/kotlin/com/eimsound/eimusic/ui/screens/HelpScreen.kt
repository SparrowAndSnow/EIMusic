package com.eimsound.eimusic.ui.screens

import androidx.compose.runtime.Composable
import com.eimsound.eimusic.keyboard.HelpAction
import com.eimsound.eimusic.keyboard.helpStatusBarHint
import com.eimsound.eimusic.keyboard.mainHelpEntries
import com.eimsound.eimusic.keyboard.mapHelpKey
import com.eimsound.eimusic.ui.components.DialogScreen
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.eimsound.eimusic.util.spacer
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text

@Composable
fun HelpScreen(
    onGoBack: () -> Unit,
) {
    val palette = LocalTuiColors.current
    DialogScreen(
        title = "Keyboard Controls",
        hint = helpStatusBarHint(),
        onDismiss = onGoBack,
        onKey = { event ->
            if (mapHelpKey(event.key) == HelpAction.Dismiss) {
                onGoBack()
                true
            } else {
                false
            }
        },
    ) {
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

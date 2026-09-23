package com.eimsound.eimusic.util

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

@Composable
actual fun onBackHandler(
    isBackEnabled: Boolean,
    onBackCancelled: () -> Unit,
    onBackCompleted: () -> Unit
) {
    if (!isBackEnabled) return

    val focusRequester = remember { FocusRequester() }

    Box(
        Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                    onBackCompleted()
                    true
                } else false
            }
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}
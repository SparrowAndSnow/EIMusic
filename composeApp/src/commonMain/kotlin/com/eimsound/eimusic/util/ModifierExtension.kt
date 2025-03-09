package com.eimsound.eimusic.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun Modifier.thenIf(condition: Boolean, modifier: @Composable Modifier.() -> Modifier) =
    if (condition) then(modifier()) else this
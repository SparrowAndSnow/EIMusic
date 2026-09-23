package com.eimsound.eimusic.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <T> StateFlow<T>.bridgeToMutable(): MutableState<T> {
    val state = remember { mutableStateOf(this.value) }
    LaunchedEffect(this) {
        collect { state.value = it }
    }
    return state
}

package com.eimsound.eimusic.util

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

object CurrentInfo : NavigationEventInfo()
object PreviousInfo : NavigationEventInfo()
@Composable
actual fun onBackHandler(
    isBackEnabled: Boolean,
    onBackCancelled: () -> Unit,
    onBackCompleted: () -> Unit
) {
    val navEventState = rememberNavigationEventState(
        currentInfo = CurrentInfo,
        backInfo = listOf(PreviousInfo)
    )
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = true,
        onBackCancelled = onBackCancelled,
        onBackCompleted = onBackCompleted,
    )
}
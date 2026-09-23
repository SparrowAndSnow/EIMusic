package com.eimsound.eimusic.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable

@Serializable
data class ToastMessage(
    val message: String,
    val actionLabel: String? = null,
    val dismissAction: Boolean = false,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val action: (() -> Unit)? = null,
)

object Toast {
    private val _messages = MutableSharedFlow<ToastMessage>()
    val messages = _messages.asSharedFlow()

    suspend fun show(message: ToastMessage) {
        _messages.emit(message)
    }
}

// 2. 建立 CompositionLocal
val LocalToastMessage = staticCompositionLocalOf<Toast> {
    error("No GlobalMessageManager provided")
}
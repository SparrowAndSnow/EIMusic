package com.eimsound.eimusic

import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.eimsound.eimusic.EIMusicApplication.Companion.instance
import com.eimsound.eimusic.viewmodel.PlayerViewModel
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class EIMusicMediaController : KoinComponent {
    private var _controller: MediaController? = null
    val mediaController: MediaController? get() = _controller
    private val playerViewModel: PlayerViewModel by inject()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    fun connect() {
        val sessionToken = SessionToken(
            instance,
            ComponentName(instance, EIMusicMediaSessionService::class.java)
        )
        val controllerFuture = MediaController.Builder(instance, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                _controller = controllerFuture.get()
                _isConnected.value = true
            } catch (e: Exception) {
                // 异常处理
            }
        }, MoreExecutors.directExecutor())
    }
}
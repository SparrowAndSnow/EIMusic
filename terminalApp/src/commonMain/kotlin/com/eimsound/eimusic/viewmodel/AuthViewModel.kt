package com.eimsound.eimusic.viewmodel

import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.auth.AuthStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val authStore: AuthStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val authState: StateFlow<AuthState> = authStore.state

    init {
        scope.launch { authStore.check() }
    }

    fun login(username: String) {
        if (username.isBlank()) return
        scope.launch { authStore.login(username) }
    }

    fun logout() {
        scope.launch { authStore.logout() }
    }

    fun release() { scope.cancel() }
}

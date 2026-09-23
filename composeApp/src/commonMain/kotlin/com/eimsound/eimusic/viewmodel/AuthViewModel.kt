package com.eimsound.eimusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.auth.AuthState
import com.eimsound.eimusic.auth.AuthStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 用户认证状态
 */

/**
 * 用户认证ViewModel
 */
class AuthViewModel(
    private val authStore: AuthStore,
) : ViewModel() {
    val authState: StateFlow<AuthState> = authStore.state

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        viewModelScope.launch {
            authStore.check()
        }
    }

    /**
     * 用户登录
     */
    fun login(username: String) {
        if (username.isBlank()) {
            return
        }

        viewModelScope.launch {
            authStore.login(username)
        }
    }

    /**
     * 用户登出
     */
    fun logout() {
        viewModelScope.launch {
            authStore.logout()
        }
    }

}
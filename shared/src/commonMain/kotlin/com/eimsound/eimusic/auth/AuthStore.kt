package com.eimsound.eimusic.auth

import com.eimsound.eimusic.network.AuthApi
import com.eimsound.eimusic.network.models.NetworkResult
import com.eimsound.eimusic.storage.Auth
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.get
import com.eimsound.eimusic.storage.save
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthState {
    object Unauthenticated : AuthState
    object Loading : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthStore(
    private val authApi: Lazy<AuthApi>,
    private val storage: IStorage,
) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun current() = storage.get(Auth.EMPTY)

    var onUnauthenticatedBlock: suspend () -> Unit = {}
     fun onUnauthenticated(block: suspend () -> Unit = {}) {
        onUnauthenticatedBlock = block
    }

    suspend fun unauthenticated() {
        onUnauthenticatedBlock()
    }

    fun store(auth: Auth) {
        storage.save(auth)
        _state.value = AuthState.Authenticated(auth.user)
    }

    fun clear() {
        storage.save(Auth.EMPTY)
        _state.value = AuthState.Unauthenticated
    }

    fun setToken(token: BearerTokens) {
        storage.save(Auth::accessToken, token.accessToken)
        token.refreshToken?.let {
            storage.save(Auth::refreshToken, it)
        }
        _state.value = AuthState.Authenticated(current().user)
    }

    fun getToken(): BearerTokens {
        return BearerTokens(
            accessToken = storage.get(Auth::accessToken, ""),
            refreshToken = storage.get(Auth::refreshToken, "")
        )
    }

    suspend fun check() {
        when (val result: NetworkResult<AuthUser> = authApi.value.getCurrentUser()) {
            is NetworkResult.Error -> {
                if (result.code == 401) {
                    _state.value = AuthState.Unauthenticated
                }
            }

            is NetworkResult.Success -> {
                _state.value = AuthState.Authenticated(result.data)
            }

            else -> {}
        }
    }

    suspend fun login(username: String): AuthState {
        _state.value = AuthState.Loading
        return when (val result: NetworkResult<AuthResponse> = authApi.value.login(username)) {
            is NetworkResult.Error -> {
                val errorMsg = result.message
                _state.value = AuthState.Error(errorMsg)
                _state.value
            }

            is NetworkResult.Exception -> {
                val errorMsg = result.message
                _state.value = AuthState.Error(errorMsg ?: "未知错误")
                _state.value
            }

            is NetworkResult.Success -> {
                val data = result.data
                store(
                    Auth(
                        accessToken = data.accessToken,
                        refreshToken = data.refreshToken,
                        expiresIn = data.expiresIn,
                        user = data.user
                    )
                )
                _state.value = AuthState.Authenticated(data.user)
                _state.value
            }
        }
    }

    suspend fun logout() {
        when (val result: NetworkResult<Unit> = authApi.value.logout()) {
            is NetworkResult.Error -> {
                val errorMsg = result.message
                _state.value = AuthState.Error(errorMsg)
            }

            is NetworkResult.Exception -> {
                val errorMsg = result.message
                _state.value = AuthState.Error(errorMsg ?: "未知错误")
            }

            is NetworkResult.Success -> {
                clear()
                _state.value = AuthState.Unauthenticated
            }
        }
    }

}
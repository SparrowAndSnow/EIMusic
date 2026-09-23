package com.eimsound.eimusic.server.manager

import com.eimsound.eimusic.server.model.UserSession

sealed interface CreateUserSessionResult {
    data class Success(val session: UserSession) : CreateUserSessionResult
    object Exists : CreateUserSessionResult
    object LimitExceeded : CreateUserSessionResult
    data class Error(val message: String) : CreateUserSessionResult
}
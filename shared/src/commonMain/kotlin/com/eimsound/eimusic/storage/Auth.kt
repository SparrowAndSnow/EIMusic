package com.eimsound.eimusic.storage

import com.eimsound.eimusic.auth.AuthUser
import kotlinx.serialization.Serializable

@Serializable
data class Auth(
    var accessToken: String = "",
    var refreshToken: String = "",
    var expiresIn: Long = 0,
    var user: AuthUser
): Store{
    companion object{
        val EMPTY = Auth("", "", 0,
            AuthUser("", "", "", ""))
    }
}
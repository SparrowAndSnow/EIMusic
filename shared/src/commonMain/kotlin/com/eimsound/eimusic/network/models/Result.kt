package com.eimsound.eimusic.network.models

import kotlinx.serialization.Serializable

@Serializable
sealed class NetworkResult<out T> {
    @Serializable
    data class Success<out T>(val data: T) : NetworkResult<T>()
    @Serializable
    data class Error(val code: Int, val message: String) : NetworkResult<Nothing>()
    @Serializable
    data class Exception(val message: String?) : NetworkResult<Nothing>()
}
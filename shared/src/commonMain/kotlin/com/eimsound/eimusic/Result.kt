package com.eimsound.eimusic

import kotlinx.serialization.Serializable

@Serializable
sealed class Result<out T> {
    @Serializable
    data class Success<out T>(val data: T) : Result<T>()

    @Serializable
    data class Error(val message: String) : Result<Nothing>()
}
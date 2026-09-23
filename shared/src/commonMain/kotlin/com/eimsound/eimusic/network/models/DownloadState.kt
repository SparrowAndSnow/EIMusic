package com.eimsound.eimusic.network.models

sealed class DownloadState {
    object None : DownloadState()
    object Loading : DownloadState()
    data class Progress(val percent: Int) : DownloadState() // 0-100
    data class Success(val path: String) : DownloadState()
    data class Error(val cause: Throwable) : DownloadState()
}
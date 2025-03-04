package com.eimsound.eimusic.network.models.topfiftycharts


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoThumbnail(
    @SerialName("url") val url: String?
)

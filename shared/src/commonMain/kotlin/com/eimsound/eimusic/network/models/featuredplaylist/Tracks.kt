package com.eimsound.eimusic.network.models.featuredplaylist


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tracks(
    @SerialName("href")
    val href: String?,
    @SerialName("total")
    val total: Int?
)

package com.eimsound.eimusic.music

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val image: String?,
    val id: String,
    val name: String,
)

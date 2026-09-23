package com.eimsound.eimusic.music

import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val artists: List<Artist>,
    val id: String,
    val image: String?,
    val name: String,
    val releaseDate: String?,
    val totalTracks: Int,
    val tracks: List<Track>
)

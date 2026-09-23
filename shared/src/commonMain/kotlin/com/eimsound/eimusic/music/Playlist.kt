package com.eimsound.eimusic.music

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val tracks: List<Track>,
    val name: String,
    val id: String,
    val creator: String?,
    val image: String?,
    val description: String?,
    val totalTracks: Int
)
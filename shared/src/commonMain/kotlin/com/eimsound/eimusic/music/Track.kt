package com.eimsound.eimusic.music

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val album: Album?,
    val artists: List<Artist>?,
    val duration: Duration?,
    val id: TrackId,
    val name: String,
    val uri: String?,
    val source: TrackSource
)

@Serializable
data class TrackId(val source: TrackSource, val id: String) {
    companion object {
        fun parse(id: String): TrackId {
            val split = id.split(":")
            return TrackId(TrackSource.valueOf(split[0]), split[1])
        }
    }

    override fun toString(): String = "$source:$id"
}
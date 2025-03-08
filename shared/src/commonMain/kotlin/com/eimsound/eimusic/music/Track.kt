package com.eimsound.eimusic.music

import com.eimsound.eimusic.Duration

data class Track(
    val album: Album?,
    val artists: List<Artist>?,
    val duration: Duration?,
    val id: String?,
    val isLocal: Boolean?,
    val name: String?,
    val previewUrl: String?,
    val uri: String?
)
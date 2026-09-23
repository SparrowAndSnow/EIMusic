package com.eimsound.eimusic.resolver.track

import com.eimsound.eimusic.music.Track

interface TrackUriResolver {
    suspend fun resolve(track: Track): String?
}
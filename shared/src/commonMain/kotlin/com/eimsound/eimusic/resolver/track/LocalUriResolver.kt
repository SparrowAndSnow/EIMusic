package com.eimsound.eimusic.resolver.track

import com.eimsound.eimusic.music.Track

class LocalUriResolver : TrackUriResolver {
    override suspend fun resolve(track: Track): String? = track.uri
}
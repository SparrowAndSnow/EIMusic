package com.eimsound.eimusic.resolver.track

import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.repository.TrackRepository
import com.eimsound.eimusic.Result
class NeteaseUriResolver(
    private val repository: TrackRepository,
) : TrackUriResolver {
    override suspend fun resolve(track: Track): String? {
        val id = track.id.id
        return when (val result = repository.track(id)) {
            is Result.Success -> {
                result.data?.uri
            }
            is Result.Error -> {
                null
            }
        }
    }
}
package com.eimsound.eimusic.factory

import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.repository.TrackRepository

object TrackRepositoryFactory {
    private val repos = mutableMapOf<TrackSource, TrackRepository>()

    fun register(source: TrackSource, repo: TrackRepository) {
        repos[source] = repo
    }

    fun get(source: TrackSource): TrackRepository =
        repos[source] ?: error("No TrackRepository registered for $source")
}

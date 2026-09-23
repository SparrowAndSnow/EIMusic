package com.eimsound.eimusic.factory

import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.resolver.track.TrackUriResolver

/**
 * Registry-based resolver lookup. Replace the old Koin-based factory.
 * Usage:
 *   // Register resolvers at startup
 *   TrackUriResolverFactory.register(TrackSource.NETEASE, neteaseResolver)
 *   TrackUriResolverFactory.register(TrackSource.LOCAL, localResolver)
 *
 *   // Get in code
 *   val resolver = TrackUriResolverFactory.get(track.source)
 */
object TrackUriResolverFactory {
    private val resolvers = mutableMapOf<TrackSource, TrackUriResolver>()

    fun register(source: TrackSource, resolver: TrackUriResolver) {
        resolvers[source] = resolver
    }

    fun get(source: TrackSource): TrackUriResolver =
        resolvers[source] ?: error("No TrackUriResolver registered for $source")
}

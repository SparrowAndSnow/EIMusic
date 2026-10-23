package com.eimsound.eimusic.factory

import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.resolver.share.ShareLinkResolver

object ShareLinkResolverFactory {
    private val domainToSource = mapOf(
        "music.163.com" to TrackSource.NETEASE,
        "y.qq.com" to TrackSource.QQ,
        "music.xiami.com" to TrackSource.XIAMI,
        "music.kuwo.cn" to TrackSource.KUWO,
        "y.kugou.com" to TrackSource.KUGOU,
        "music.baidu.com" to TrackSource.BAIDU,
        "music.youtube.com" to TrackSource.YOUTUBE,
    )

    private val resolvers = mutableMapOf<TrackSource, ShareLinkResolver>()

    fun register(source: TrackSource, resolver: ShareLinkResolver) {
        resolvers[source] = resolver
    }

    fun get(source: TrackSource): ShareLinkResolver =
        resolvers[source] ?: error("No ShareLinkResolver registered for $source")

    fun get(uri: String): ShareLinkResolver {
        val source = getSource(uri)
        return get(source)
    }

    fun getSource(uri: String): TrackSource {
        val host = uri.substringAfter("://", "").substringBefore('/').substringBefore('?')
        return domainToSource[host] ?: TrackSource.UNKNOWN
    }
}

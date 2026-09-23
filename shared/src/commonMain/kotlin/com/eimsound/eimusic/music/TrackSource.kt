package com.eimsound.eimusic.music

import kotlinx.serialization.Serializable

@Serializable
enum class TrackSource {
    LOCAL,
    NETEASE,
    QQ,
    XIAMI,
    KUWO,
    KUGOU,
    BAIDU,
    YOUTUBE,
    SPOTIFY,

    YOUTUBE_MUSIC,

    UNKNOWN
}
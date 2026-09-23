package com.eimsound.eimusic.views.components.setting

import androidx.compose.runtime.Composable
import com.eimsound.eimusic.music.TrackSource
import eimusic.composeapp.generated.resources.Res
import eimusic.composeapp.generated.resources.config_source_baidu
import eimusic.composeapp.generated.resources.config_source_kugou
import eimusic.composeapp.generated.resources.config_source_kuwo
import eimusic.composeapp.generated.resources.config_source_netease
import eimusic.composeapp.generated.resources.config_source_qq
import eimusic.composeapp.generated.resources.config_source_spotify
import eimusic.composeapp.generated.resources.config_source_xiami
import eimusic.composeapp.generated.resources.config_source_youtube
import eimusic.composeapp.generated.resources.config_source_youtube_music
import org.jetbrains.compose.resources.stringResource

/** TrackSource 的可读显示名（AppConfig 编辑器用；LOCAL / UNKNOWN 无源 URL 概念，兜底用枚举名） */
@Composable
fun TrackSource.displayLabel(): String {
    val res = when (this) {
        TrackSource.NETEASE -> Res.string.config_source_netease
        TrackSource.SPOTIFY -> Res.string.config_source_spotify
        TrackSource.QQ -> Res.string.config_source_qq
        TrackSource.XIAMI -> Res.string.config_source_xiami
        TrackSource.KUWO -> Res.string.config_source_kuwo
        TrackSource.KUGOU -> Res.string.config_source_kugou
        TrackSource.BAIDU -> Res.string.config_source_baidu
        TrackSource.YOUTUBE -> Res.string.config_source_youtube
        TrackSource.YOUTUBE_MUSIC -> Res.string.config_source_youtube_music
        else -> return name
    }
    return stringResource(res)
}

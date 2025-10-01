package com.eimsound.eimusic.components

import TrackListViewModel
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.eimsound.eimusic.music.Artist
import com.eimsound.eimusic.music.Track
import org.koin.compose.viewmodel.koinViewModel
import kotlin.reflect.KProperty1

/**
 * 通用的曲目列表组件，用于显示任何曲目集合（如专辑、歌单等）
 * @param tracks 要显示的曲目列表
 * @param modifier 修饰符
 * @param key 用于区分列表项的键
 * @param onPlayTrack 当用户想要播放某首曲目时的回调
 * @param onTrackNameClick 当用户点击曲目名称时的回调
 * @param onArtistClick 当用户点击艺术家时的回调
 * @param onAddToPlaylist 当用户想要将曲目添加到播放列表时的回调
 * @param onFavorite 当用户想要收藏曲目时的回调
 * @param onDownload 当用户想要下载曲目时的回调
 * @param onShare 当用户想要分享曲目时的回调
 */
@Composable
fun TrackList(
    tracks: List<Track>,
    modifier: Modifier = Modifier,
    key: KProperty1<Track, *> = Track::id,
    onPlayTrack: ((Track) -> Unit)? = null,
    onTrackNameClick: ((Track) -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,
    onAddToPlaylist: ((Track) -> Unit)? = null,
    onFavorite: ((Track) -> Unit)? = null,
    onDownload: ((Track) -> Unit)? = null,
    onShare: ((Track) -> Unit)? = null
) {
    val lazyListState = rememberLazyListState()
    val trackListViewModel = koinViewModel<TrackListViewModel>()
    val state by trackListViewModel.state.collectAsState()

    ColumnList(
        modifier = modifier,
        state = lazyListState,
        list = tracks,
        key = key
    ) { track ->
        TrackItem(
            track = track,
            isPlaying = track == state.currentTrack, // TrackList 不显示当前播放状态
            onPlayClick = {
                trackListViewModel.play(tracks, track)
                onPlayTrack?.invoke(track)
            },
            onTrackNameClick = {
                onTrackNameClick?.invoke(track)
            },
            onArtistClick = onArtistClick ?: {},
            onAddToPlaylist = {
                trackListViewModel.addToQueue(track)
                onAddToPlaylist?.invoke(track)
            },
            onFavorite = {
                onFavorite?.invoke(track)
            },
            onDownload = {
                onDownload?.invoke(track)
            },
            onShare = {
                onShare?.invoke(track)
            }
        )
    }
}
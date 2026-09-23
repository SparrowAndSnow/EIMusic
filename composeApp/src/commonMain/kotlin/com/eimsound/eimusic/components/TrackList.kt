package com.eimsound.eimusic.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.Artist
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.theme.LocalPalette
import com.eimsound.eimusic.util.AutoHideHeaderState
import com.eimsound.eimusic.util.rememberAutoHideHeaderState
import com.eimsound.eimusic.viewmodel.TrackListViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.reflect.KProperty1

@Composable
fun TrackList(
    tracks: List<Track>,
    autoHideHeaderState: AutoHideHeaderState = rememberAutoHideHeaderState(),
    header: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    refreshingEnabled: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    key: KProperty1<Track, *> = Track::id,
    onPlayTrack: ((Track) -> Unit)? = null,
    onTrackNameClick: ((Track) -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,
    onAddToPlaylist: ((Track, AddTrackStrategy) -> Unit)? = null,
    onFavorite: ((Track) -> Unit)? = null,
    onDownload: ((Track) -> Unit)? = null,
    onShare: ((Track) -> Unit)? = null
) {
    val palette = LocalPalette.current
    val lazyListState = rememberLazyListState()
    val trackListViewModel = koinViewModel<TrackListViewModel>()
    val state by trackListViewModel.state.collectAsState()
    val clipboard = LocalClipboard.current
    Column(modifier = modifier) {
        ColumnList(
            header = header,
            autoHideHeaderState = autoHideHeaderState,
            state = lazyListState,
            refreshingEnabled = refreshingEnabled,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            list = tracks,
            key = key,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp)
        ) { track ->
            TrackItem(
                track = track,
                index = tracks.indexOf(track) + 1,
                isPlaying = track.id == state.current?.id,
                onPlayClick = {
                    trackListViewModel.play(tracks, track)
                    onPlayTrack?.invoke(track)
                },
                onTrackNameClick = {
                    trackListViewModel.play(tracks, track)
                    onTrackNameClick?.invoke(track)
                },
                onArtistClick = onArtistClick ?: {},
                onAddToPlaylist = { track, strategy ->
                    trackListViewModel.addToQueue(track, strategy)
                    onAddToPlaylist?.invoke(track, strategy)
                },
                onFavorite = {
                    onFavorite?.invoke(track)
                },
                onDownload = {
                    onDownload?.invoke(track)
                },
                onShare = {
                    trackListViewModel.share(clipboard, track)
                    onShare?.invoke(track)
                }
            )
        }
    }
}

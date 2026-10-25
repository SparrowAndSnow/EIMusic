package com.eimsound.eimusic.viewmodel

import androidx.compose.ui.platform.Clipboard
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eimsound.eimusic.events.EventBus
import com.eimsound.eimusic.events.PlayingListEvent
import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.music.TrackSource
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.playback.PlaybackStore
import com.eimsound.eimusic.util.Toast
import com.eimsound.eimusic.util.ToastMessage
import com.eimsound.eimusic.util.clipEntryOf
import kotlinx.coroutines.launch

/** 列表 UI 适配：播放/入队直接走共享 PlaybackStore，避免依赖 EventBus 时序。 */
class TrackListViewModel(
    val controller: IMediaPlayerController,
    private val playbackStore: PlaybackStore,
) : ViewModel() {
    val state = controller.state

    fun play(tracks: List<Track>, track: Track) {
        playbackStore.playTrackFromList(tracks, track)
    }

    fun addToQueue(track: Track, strategy: AddTrackStrategy = AddTrackStrategy.AddToLast) {
        playbackStore.addToQueue(track, strategy)
    }

    fun share(clipboard: Clipboard, track: Track) {
        viewModelScope.launch {
            if (track.source == TrackSource.LOCAL) return@launch
            val clipEntry = clipEntryOf(track.id.toString())
            clipboard.setClipEntry(clipEntry)
            Toast.show(ToastMessage("已复制歌曲ID到剪贴板"))
        }
    }
}

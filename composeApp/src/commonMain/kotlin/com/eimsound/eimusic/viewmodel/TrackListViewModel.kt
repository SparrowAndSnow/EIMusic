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
import com.eimsound.eimusic.util.Toast
import com.eimsound.eimusic.util.ToastMessage
import com.eimsound.eimusic.util.clipEntryOf
import kotlinx.coroutines.launch

/**
 * 通用的 TrackListViewModel，支持不同类型的音轨列表展示
 */
class TrackListViewModel(val controller: IMediaPlayerController) : ViewModel() {

    val state = controller.state

    /**
     * 播放音轨
     */
    fun play(tracks: List<Track>, track: Track) {
        EventBus.PlayingListEventBus.send(PlayingListEvent.TrackListChanged(tracks, track))
    }

    /**
     * 添加到播放队列
     */
    fun addToQueue(track: Track, strategy: AddTrackStrategy = AddTrackStrategy.AddToLast) {
        EventBus.PlayingListEventBus.send(PlayingListEvent.TrackAdded(track, strategy))
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
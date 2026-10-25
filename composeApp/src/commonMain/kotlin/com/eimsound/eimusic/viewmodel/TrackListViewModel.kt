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
import com.eimsound.eimusic.together.ChannelState
import com.eimsound.eimusic.together.TogetherSyncManager
import com.eimsound.eimusic.util.Toast
import com.eimsound.eimusic.util.ToastMessage
import com.eimsound.eimusic.util.clipEntryOf
import kotlinx.coroutines.launch

/**
 * 通用的 TrackListViewModel，支持不同类型的音轨列表展示
 */
class TrackListViewModel(
    val controller: IMediaPlayerController,
    private val syncManager: TogetherSyncManager,
) : ViewModel() {

    private fun togetherJoined(): Boolean =
        syncManager.channelState.value is ChannelState.Joined

    private fun rejectLocalTrack(track: Track): Boolean {
        if (togetherJoined() && track.source == TrackSource.LOCAL) {
            viewModelScope.launch {
                Toast.show(ToastMessage("本地曲目无法一起听，请切换到在线曲目"))
            }
            return true
        }
        return false
    }


    val state = controller.state

    /**
     * 播放音轨
     */
    fun play(tracks: List<Track>, track: Track) {
        if (rejectLocalTrack(track)) return
        val playable = if (togetherJoined()) {
            tracks.filter { it.source != TrackSource.LOCAL }
        } else {
            tracks
        }
        if (playable.isEmpty()) {
            if (tracks.isNotEmpty()) rejectLocalTrack(track)
            return
        }
        EventBus.PlayingListEventBus.send(PlayingListEvent.TrackListChanged(playable, track))
    }

    /**
     * 添加到播放队列
     */
    fun addToQueue(track: Track, strategy: AddTrackStrategy = AddTrackStrategy.AddToLast) {
        if (rejectLocalTrack(track)) return
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
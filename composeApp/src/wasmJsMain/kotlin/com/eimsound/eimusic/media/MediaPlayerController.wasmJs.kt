package com.eimsound.eimusic.media

import com.eimsound.eimusic.events.PlaybackEvent
import com.eimsound.eimusic.music.PlayMode
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.together.PlayState
import com.eimsound.eimusic.together.SyncState
import com.eimsound.eimusic.factory.TrackUriResolverFactory
import com.eimsound.eimusic.playback.IMediaPlayerController
import com.eimsound.eimusic.playback.PlaybackOrigin
import kotlinx.browser.document
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.w3c.dom.HTMLAudioElement

/**
 * Web平台的媒体播放控制器实现
 * 目前仅作为占位符，待后续实现完整功能
 */
actual class MediaPlayerController actual constructor(trackUriResolverFactory: TrackUriResolverFactory) :
    IMediaPlayerController {
    private var audio: HTMLAudioElement? = null
    private var isPrepared = false
    private val _event = MutableSharedFlow<PlaybackEvent>(replay = 0)
    actual override val event: SharedFlow<PlaybackEvent> = _event.asSharedFlow()

    @OptIn(ExperimentalWasmJsInterop::class)
    fun prepare(media: Track) {
        val element = audio ?: (document.createElement("audio") as HTMLAudioElement).also { audio = it }

        // 监听加载完成
        element.oncanplay = {
            isPrepared = true
        }

        // 监听播放结束
        element.onended = {
        }

        // 监听错误
        element.onerror?.let {
        }

        element.onplaying?.let {
        }

        element.onpause?.let {
        }
    }


    actual override fun queue(medias: List<Track>, prepareOver: () -> Unit) {
    }

    actual override fun play(media: Track, origin: PlaybackOrigin) {
    }

    actual override fun next(isAudioCompleted: Boolean, origin: PlaybackOrigin) {
    }

    actual override fun previous(origin: PlaybackOrigin) {
    }

    actual override fun playState(state: PlayState, origin: PlaybackOrigin) {
    }

    actual override fun playMode(mode: PlayMode, origin: PlaybackOrigin) {
    }

    actual override fun volume(volume: Float, isMuted: Boolean) {
    }

    actual override fun seek(millis: Long, seekOver: () -> Unit, origin: PlaybackOrigin) {
    }


    actual override val state: StateFlow<PlayerState>
        get() = TODO("Not yet implemented")

    actual override fun release() {
    }

    actual override fun nextTrack(): Track? {
        TODO("Not yet implemented")
    }

    actual override fun previousTrack(): Track? {
        TODO("Not yet implemented")
    }

    actual override fun sync(syncState: SyncState) {
    }

    actual override fun addTrack(media: Track, index: Int) {
    }

    actual override fun updateQueue(tracks: List<Track>) {}

    actual override fun moveTrack(from: Int, to: Int) {}

    actual override fun removeTrack(index: Int) {
    }
}

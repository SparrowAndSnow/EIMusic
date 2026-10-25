package com.eimsound.eimusic.viewmodel

import com.eimsound.eimusic.music.AddTrackStrategy
import com.eimsound.eimusic.music.PlayerState
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.playback.PlaybackStore
import kotlinx.coroutines.flow.StateFlow

/** terminalApp 的薄壳：所有播放/队列/持久化逻辑在 shared PlaybackStore。 */
class PlayerViewModel(
    private val store: PlaybackStore,
) {
    val state: StateFlow<PlayerState> = store.state
    val recentTracks: StateFlow<List<Track>> = store.recentTracks
    val displayPosition: StateFlow<Long> = store.displayPosition

    fun seekRelative(delta: Long) = store.seekRelative(delta)
    fun togglePlayPause() = store.togglePlayPause()
    fun next() = store.next()
    fun previous() = store.previous()
    fun seek(millis: Long) = store.seek(millis)
    fun togglePlayMode() = store.togglePlayMode()
    fun volumeUp() = store.volumeUp()
    fun adjustVolume(delta: Int) = store.adjustVolume(delta)
    fun volumeDown() = store.volumeDown()
    fun toggleMute() = store.toggleMute()

    fun playNext(track: Track) = store.playNext(track)
    fun addToQueue(track: Track, strategy: AddTrackStrategy = AddTrackStrategy.AddToLast) =
        store.addToQueue(track, strategy)
    fun playTrackFromList(tracks: List<Track>, track: Track) = store.playTrackFromList(tracks, track)
    fun playTrack(track: Track) = store.playTrack(track)
    fun playAll(tracks: List<Track>) = store.playAll(tracks)
    fun removeTrack(index: Int) = store.removeTrack(index)
    fun moveTrack(from: Int, to: Int) = store.moveTrack(from, to)
    fun clearQueue() = store.clearQueue()

    fun release() = store.release()
}

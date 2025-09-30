package com.eimsound.eimusic.media

import com.eimsound.eimusic.media.PlayMode
import com.eimsound.eimusic.music.Track

/**
 * 播放控制接口，用于解耦播放器和播放列表
 */
interface PlaybackController {
    /**
     * 播放下一首
     */
    fun next(playMode: PlayMode)
    
    /**
     * 播放上一首
     */
    fun previous(playMode: PlayMode)
    
    /**
     * 播放指定曲目
     */
    fun play(track: Track)
    
    /**
     * 加载播放列表
     */
    fun load(tracks: List<Track>)
}
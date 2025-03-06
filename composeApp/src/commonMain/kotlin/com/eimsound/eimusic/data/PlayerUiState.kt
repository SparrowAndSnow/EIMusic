package com.eimsound.eimusic.data

import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.media.PlayMode
import com.eimsound.eimusic.network.models.topfiftycharts.Item

data class PlayerUiState(

    val isLoading: Boolean = false,
    val position: Duration = Duration(0),
    val playMode: PlayMode = PlayMode.LOOP,
    val volume: Double = 1.0,
    val isMute: Boolean = false,
    val isPlaying: Boolean = false,

)

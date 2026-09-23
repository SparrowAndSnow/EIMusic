package com.eimsound.eimusic.together

sealed interface ChannelState {
    data class Joined(val channel: Channel) : ChannelState
    data class Joining(val channelId: String) : ChannelState
    object Disconnected : ChannelState
    data class JoiningFailed(val error: String) : ChannelState
}
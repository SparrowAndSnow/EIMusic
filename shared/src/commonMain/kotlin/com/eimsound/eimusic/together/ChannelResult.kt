package com.eimsound.eimusic.together

sealed interface CreateChannelResult {
    data class Success(val channel: Channel) : CreateChannelResult
    data class Error(val message: String) : CreateChannelResult
    data class Exists(val channel: Channel) : CreateChannelResult
    object LimitExceeded : CreateChannelResult
}

sealed interface JoinChannelResult {
    data class Success(val channel: Channel) : JoinChannelResult
    data class Error(val message: String) : JoinChannelResult
    object NotFound : JoinChannelResult
    data class AlreadyInChannel(val channel: Channel) : JoinChannelResult
    object ChannelFull : JoinChannelResult
}

sealed interface LeaveChannelResult {
    data class Success(val channel: Channel) : LeaveChannelResult
    data class Error(val message: String) : LeaveChannelResult
    object NotFound : LeaveChannelResult
    object NotInChannel : LeaveChannelResult
    data class SuccessAndChannelEmpty(val channel: Channel) : LeaveChannelResult
}
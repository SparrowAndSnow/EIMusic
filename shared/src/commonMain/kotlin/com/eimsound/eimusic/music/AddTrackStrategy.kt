package com.eimsound.eimusic.music

import kotlinx.serialization.Serializable


@Serializable
sealed interface AddTrackStrategy {
    @Serializable
    object AddToNext: AddTrackStrategy
    @Serializable
    object AddToLast: AddTrackStrategy
    @Serializable
    data class InsertToQueue(val index: Int): AddTrackStrategy
}
package com.eimsound.eimusic.media

enum class PlayMode {
    SHUFFLE,
    LOOP,
    REPEAT_ONE;

    fun change() = when(this){
        SHUFFLE -> LOOP
        LOOP -> REPEAT_ONE
        REPEAT_ONE -> SHUFFLE
    }
}

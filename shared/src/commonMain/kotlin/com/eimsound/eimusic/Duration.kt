package com.eimsound.eimusic

data class Duration(val seconds: Long) {
    fun percentOf(percent: Float): Duration {
        if(percent > 1F) return Duration(seconds)
        return Duration((seconds * percent).toLong())
    }

    fun parsePercent(current: Long): Float {
        if (seconds == 0L || current > seconds) return 0f
        return current.toFloat() / seconds
    }

    val hoursPart
        get() = seconds / 3600
    val minutesPart
        get() = (seconds % 3600) / 60
    val secondsPart
        get() = seconds % 60
}
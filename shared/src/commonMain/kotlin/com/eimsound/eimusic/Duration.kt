package com.eimsound.eimusic

data class Duration(val seconds: Long) {
    fun percentOf(percent: Float): Duration {
        if(percent > 1F) return Duration(seconds)
        return Duration((seconds * percent).toLong())
    }

    fun toPercent(current: Duration): Float {
        if (seconds <= 0L || current.seconds > seconds) return 0F
        return current.seconds.toFloat() / seconds
    }

    val hoursPart
        get() = seconds / 3600
    val minutesPart
        get() = (seconds % 3600) / 60
    val secondsPart
        get() = seconds % 60
}

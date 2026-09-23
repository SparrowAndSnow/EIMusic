package com.eimsound.eimusic.music

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Duration(val milliseconds: Long) {
    fun percentOf(percent: Float): Duration {
        return Duration((milliseconds * percent.coerceIn(0F, 1F)).toLong())
    }

    fun toPercent(current: Duration): Float {
        if (milliseconds <= 0) return 0F
        val value = current.milliseconds.coerceIn(0, milliseconds).toFloat() / milliseconds
        return value
    }

    val hoursPart get() = milliseconds / 3_600_000L
    val minutesPart get() = (milliseconds % 3_600_000L) / 60_000L
    val secondsPart get() = (milliseconds % 60_000L) / 1000L

    val seconds: Long
        get() = milliseconds / 1000L

    override fun toString(): String {
        return "${minutesPart}:${secondsPart.toString().padStart(2, '0')}"
    }
}
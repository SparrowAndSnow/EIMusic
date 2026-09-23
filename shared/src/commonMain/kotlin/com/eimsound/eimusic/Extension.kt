package com.eimsound.eimusic

fun Int.cycle(min: Int, max: Int): Int {
    return when {
        this > max -> min
        this < min -> max
        else -> this
    }
}
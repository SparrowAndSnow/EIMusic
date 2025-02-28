package com.eimsound.eimusic

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
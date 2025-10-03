package com.eimsound.eimusic.util

import com.eimsound.eimusic.music.Track

actual suspend fun loadTrackFiles(dirs: Set<String>): List<Track> {
    return emptyList()
}
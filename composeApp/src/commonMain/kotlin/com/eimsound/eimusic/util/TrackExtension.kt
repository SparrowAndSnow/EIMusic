package com.eimsound.eimusic.util

import com.eimsound.eimusic.music.Track

expect suspend fun loadTrackFiles(dirs: List<String>): List<Track>

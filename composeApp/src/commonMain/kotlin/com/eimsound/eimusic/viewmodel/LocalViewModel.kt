package com.eimsound.eimusic.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.data.Storage
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.settings.Settings

class LocalViewModel(storage: Storage): ViewModel() {
    val tracks by mutableStateOf<List<Track>>(emptyList())

    init {
//        tracks = storage.get(Settings::localPath, emptyList())
    }
}

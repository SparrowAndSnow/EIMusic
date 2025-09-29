package com.eimsound.eimusic.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.eimsound.eimusic.data.Storage
import com.eimsound.eimusic.music.Track
import com.eimsound.eimusic.settings.Settings
import kotlin.time.ExperimentalTime



@OptIn(ExperimentalTime::class)
class LocalViewModel(val storage: Storage) : ViewModel() {
    var tracks by mutableStateOf<List<Track>>(emptyList())
    val localPaths by mutableStateOf<List<String>>(storage.get(Settings::localPath, emptyList()))


}

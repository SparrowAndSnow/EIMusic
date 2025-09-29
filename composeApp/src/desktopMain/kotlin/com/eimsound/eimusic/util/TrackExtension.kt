package com.eimsound.eimusic.util

import com.eimsound.eimusic.Duration
import com.eimsound.eimusic.music.Track
import java.io.File
import java.net.URI

val audioExtensions = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "wma")

actual suspend fun loadTrackFiles(dirs: List<String>): List<Track> {
    return dirs.flatMap { dir ->
        
        getAudioFilesRecursively(dir).map { it ->
            Track(
                id = it.path,
                name = it.name,
                uri = it.toURI().toString(),
                artists = null,
                album = null,
                duration = Duration(60),
                isLocal = true,
            )
        }
    }
}

fun getAudioFilesRecursively(directoryPath: String): List<File> {
    val directory = File(directoryPath)
    if (!directory.exists()) return emptyList()
    val result = mutableListOf<File>()
    directory.walk().forEach { file ->
        if (file.isFile && file.extension.lowercase() in audioExtensions) {
            result.add(file)
        }
    }
    return result
}
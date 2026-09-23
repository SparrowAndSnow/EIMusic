package com.eimsound.eimusic.util

import com.eimsound.eimusic.music.Track
import java.io.File


actual suspend fun loadTrackFiles(dirs: Set<String>): List<Track> {
    return dirs.flatMap { dir ->
        getAudioFilesRecursively(dir).map { it ->
            localTack(it.absolutePath, it.name, it.toURI().toString())
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
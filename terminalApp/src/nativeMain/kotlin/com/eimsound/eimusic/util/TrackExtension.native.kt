package com.eimsound.eimusic.util

import com.eimsound.eimusic.music.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

actual suspend fun loadTrackFiles(dirs: Set<String>): List<Track> = withContext(Dispatchers.Default) {
    if (dirs.isEmpty()) return@withContext emptyList()

    val result = mutableListOf<Track>()
    for (dir in dirs) {
        scanDirectory(Path(dir), result)
    }
    result
}

private fun scanDirectory(dir: Path, result: MutableList<Track>) {
    if (!SystemFileSystem.exists(dir)) return
    if (SystemFileSystem.metadataOrNull(dir)?.isDirectory != true) return

    val entries = runCatching { SystemFileSystem.list(dir) }.getOrNull() ?: return

    for (entry in entries) {
        val name = entry.name
        if (name == "." || name == "..") continue

        val meta = SystemFileSystem.metadataOrNull(entry)
        if (meta?.isDirectory == true) {
            scanDirectory(entry, result)
        } else if (meta?.isRegularFile == true) {
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext in audioExtensions) {
                result.add(localTrack(entry.toString()))
            }
        }
    }
}

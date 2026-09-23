package com.eimsound.eimusic.util

import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import com.eimsound.eimusic.EIMusicApplication
import com.eimsound.eimusic.music.Track

actual suspend fun loadTrackFiles(dirs: Set<String>): List<Track> {
    return queryAllAudio(EIMusicApplication.instance)
}

fun queryAllAudio(context: Context): List<Track> {
    val result = mutableListOf<Track>()

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DATA
    )

    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        null, null, null
    )?.use { cursor ->
        val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        while (cursor.moveToNext()) {
            val path = cursor.getString(pathColumn)
            val track = localTack(path, path.substringAfterLast('/'), path.toUri().toString())
            result.add(track)
        }
    }
    return result
}
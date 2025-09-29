package com.eimsound.eimusic.data

import com.eimsound.eimusic.media.PlayMode
import com.eimsound.eimusic.settings.Settings
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

actual class Storage {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val fileName = "config.json"
    private var settings: Settings = Settings(
        playMode = PlayMode.LOOP.toString(),
        volume = 1.0,
        isMuted = false,
        localPath = emptyList(),
        darkMode = false,
        themeFollowSystem = true,
        language = "zh"
    )

    private fun load() {
        val configText = File(fileName).apply {
            if (!exists()) {
                createNewFile()
                writeText(json.encodeToString(settings))
            }
        }.readText()
        try{
            settings = json.decodeFromString<Settings>(configText)
        } catch (e: Exception) {}
    }

    init {
        load()
    }

    actual fun <T, V> save(key: KMutableProperty1<T, V>, value: V) {
        key.set(settings as T, value)
        File(fileName).writeText(json.encodeToString(settings))
    }

    actual fun <T, V> get(key: KProperty1<T, *>, defaultValue: V): V {
        return key.get(settings as T) as V ?: defaultValue
    }
}
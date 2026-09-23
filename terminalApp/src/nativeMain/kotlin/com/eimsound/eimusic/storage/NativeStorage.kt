package com.eimsound.eimusic.storage

import com.eimsound.eimusic.config.AppConfig
import com.eimsound.eimusic.defaultConfigDir
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Store
import com.eimsound.eimusic.storage.storeKey
import com.eimsound.eimusic.storage.storageJson
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.reflect.KClass

class NativeStorage(
    private val basePath: String = defaultConfigDir()
) : IStorage {
    init {
        SystemFileSystem.createDirectories(Path(basePath))
        ensureConfigFile()
    }

    private fun ensureConfigFile() {
        val file = Path("$basePath/${storeKey(AppConfig::class)}")
        if (!SystemFileSystem.exists(file)) {
            write(AppConfig::class, storageJson.encodeToString(AppConfig.serializer(), AppConfig()).encodeToByteArray())
        }
    }

    override fun <T : Store> read(clazz: KClass<T>): ByteArray? {
        val path = Path("$basePath/${storeKey(clazz)}")
        if (!SystemFileSystem.exists(path)) return null
        return runCatching {
            SystemFileSystem.source(path).buffered().use { src ->
                val out = mutableListOf<Byte>()
                while (!src.exhausted()) {
                    out.add(src.readByte())
                }
                out.toByteArray()
            }
        }.getOrNull()
    }

    override fun <T : Store> write(clazz: KClass<T>, data: ByteArray) {
        runCatching {
            SystemFileSystem.sink(Path("$basePath/${storeKey(clazz)}")).buffered().use { sink ->
                sink.write(data)
                sink.flush()
            }
        }
    }
}

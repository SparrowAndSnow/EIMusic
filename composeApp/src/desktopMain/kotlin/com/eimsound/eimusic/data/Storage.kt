package com.eimsound.eimusic.data

import com.eimsound.eimusic.config.AppConfig
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Store
import com.eimsound.eimusic.storage.storageJson
import com.eimsound.eimusic.storage.storeKey
import java.io.File
import kotlin.reflect.KClass

actual class Storage : IStorage {
    private val baseDir = File("${System.getProperty("user.home")}/.config/eimusic").also { it.mkdirs() }

    init {
        ensureConfigFile()
    }

    private fun ensureConfigFile() {
        val file = File(baseDir, storeKey(AppConfig::class))
        if (!file.exists()) {
            write(AppConfig::class, storageJson.encodeToString(AppConfig.serializer(), AppConfig()).encodeToByteArray())
        }
    }

    actual override fun <T : Store> read(clazz: KClass<T>): ByteArray? {
        val file = File(baseDir, storeKey(clazz))
        return if (file.exists()) file.readBytes() else null
    }

    actual override fun <T : Store> write(clazz: KClass<T>, data: ByteArray) {
        File(baseDir, storeKey(clazz)).writeBytes(data)
    }
}

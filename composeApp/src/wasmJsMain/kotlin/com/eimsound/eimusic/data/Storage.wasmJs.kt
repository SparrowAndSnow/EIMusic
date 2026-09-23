package com.eimsound.eimusic.data

import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Store
import com.eimsound.eimusic.storage.storeKey
import kotlin.reflect.KClass

actual class Storage : IStorage {
    private val data = mutableMapOf<String, ByteArray>()

    actual override fun <T : Store> read(clazz: KClass<T>): ByteArray? = data[storeKey(clazz)]

    actual override fun <T : Store> write(clazz: KClass<T>, data: ByteArray) {
        this.data[storeKey(clazz)] = data
    }
}

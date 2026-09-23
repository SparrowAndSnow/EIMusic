package com.eimsound.eimusic.data

import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Store
import kotlin.reflect.KClass

expect class Storage constructor() : IStorage {
    override fun <T : Store> read(clazz: KClass<T>): ByteArray?
    override fun <T : Store> write(clazz: KClass<T>, data: ByteArray)
}

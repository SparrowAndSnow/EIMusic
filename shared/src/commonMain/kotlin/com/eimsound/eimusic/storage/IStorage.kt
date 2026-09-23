package com.eimsound.eimusic.storage

import kotlin.reflect.KClass

interface IStorage {
    fun <T : Store> read(clazz: KClass<T>): ByteArray?
    fun <T : Store> write(clazz: KClass<T>, data: ByteArray)
}

interface Store

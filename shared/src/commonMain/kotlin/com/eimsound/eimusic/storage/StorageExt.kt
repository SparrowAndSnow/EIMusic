package com.eimsound.eimusic.storage

import com.eimsound.eimusic.config.AppConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

val storageJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun <T : Store> storeKey(clazz: KClass<T>): String = when (clazz) {
    AppConfig::class -> "eimusic.json"
    else -> clazz.simpleName?.lowercase()?.let { "$it.json" } ?: "unknown.json"
}

fun <T : Store> defaultStore(clazz: KClass<T>): T = when (clazz) {
    Settings::class -> Settings.DEFAULT as T
    Auth::class -> Auth.EMPTY as T
    AppConfig::class -> AppConfig() as T
    else -> error("Unknown Store: ${clazz.simpleName}")
}

inline fun <reified T : Store> IStorage.get(defaultValue: T): T {
    val bytes = read(T::class) ?: return defaultValue
    return try {
        storageJson.decodeFromString<T>(bytes.decodeToString())
    } catch (_: kotlinx.serialization.SerializationException) {
        defaultValue
    }
}

inline fun <reified T : Store> IStorage.save(value: T) {
    write(T::class, storageJson.encodeToString<T>(value).encodeToByteArray())
}

inline fun <reified T : Store, V> IStorage.get(key: KProperty1<T, *>, defaultValue: V): V {
    val bytes = read(T::class) ?: return defaultValue
    return try {
        key.get(storageJson.decodeFromString<T>(bytes.decodeToString())) as? V ?: defaultValue
    } catch (_: kotlinx.serialization.SerializationException) {
        defaultValue
    }
}

inline fun <reified T : Store, V> IStorage.save(key: KMutableProperty1<T, V>, value: V) {
    val obj = get(defaultStore(T::class))
    key.set(obj, value)
    save(obj)
}

package com.eimsound.eimusic.storage

import com.eimsound.eimusic.config.AppConfig
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryStorage : IStorage {
    val map = mutableMapOf<String, ByteArray>()
    override fun <T : Store> read(clazz: KClass<T>): ByteArray? = map[storeKey(clazz)]
    override fun <T : Store> write(clazz: KClass<T>, data: ByteArray) {
        map[storeKey(clazz)] = data
    }
}

class StorageTest {
    @Test
    fun wholeObjectRoundTrip() {
        val s = InMemoryStorage()
        s.save(Settings.DEFAULT.copy(volume = 0.5f))
        assertEquals(0.5f, s.get(Settings.DEFAULT).volume)
    }

    @Test
    fun missingReturnsDefault() {
        val s = InMemoryStorage()
        assertEquals(Settings.DEFAULT, s.get(Settings.DEFAULT))
    }

    @Test
    fun corruptJsonReturnsDefault() {
        val s = InMemoryStorage()
        s.map[storeKey(Settings::class)] = "not json".encodeToByteArray()
        assertEquals(Settings.DEFAULT, s.get(Settings.DEFAULT))
    }

    @Test
    fun fieldGetReturnsFieldDefaultWhenMissing() {
        val s = InMemoryStorage()
        assertEquals(1.0f, s.get(Settings::volume, 1.0f))
    }

    @Test
    fun fieldSaveKeepsOtherFields() {
        val s = InMemoryStorage()
        s.save(Settings.DEFAULT.copy(volume = 0.5f))
        s.save(Settings::isMuted, true)
        val loaded = s.get(Settings.DEFAULT)
        assertEquals(0.5f, loaded.volume)
        assertEquals(true, loaded.isMuted)
    }

    @Test
    fun appConfigUsesEimusicJsonKey() {
        val s = InMemoryStorage()
        s.save(AppConfig())
        assertTrue(s.map.containsKey("eimusic.json"))
    }
}

package com.eimsound.eimusic.data

import android.content.Context
import androidx.core.content.edit
import com.eimsound.eimusic.EIMusicApplication
import com.eimsound.eimusic.storage.IStorage
import com.eimsound.eimusic.storage.Store
import com.eimsound.eimusic.storage.storeKey
import kotlin.reflect.KClass

actual class Storage : IStorage {
    private val prefs by lazy {
        EIMusicApplication.instance.applicationContext
            .getSharedPreferences("eimusic_prefs", Context.MODE_PRIVATE)
    }

    actual override fun <T : Store> read(clazz: KClass<T>): ByteArray? {
        return prefs.getString(storeKey(clazz), null)?.encodeToByteArray()
    }

    actual override fun <T : Store> write(clazz: KClass<T>, data: ByteArray) {
        prefs.edit { putString(storeKey(clazz), data.decodeToString()) }
    }
}

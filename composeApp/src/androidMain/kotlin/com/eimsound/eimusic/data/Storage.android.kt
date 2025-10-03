package com.eimsound.eimusic.data

import android.content.Context
import android.content.SharedPreferences
import com.eimsound.eimusic.EIMusicApplication
import kotlinx.serialization.json.Json
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

actual class Storage actual constructor() {
    private val json = Json {
        ignoreUnknownKeys = true
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        val context = EIMusicApplication.instance.applicationContext
        context.getSharedPreferences("eimusic_prefs", Context.MODE_PRIVATE)
    }
    
    actual fun <T, V> save(key: KMutableProperty1<T, V>, value: V) {
        val editor = sharedPreferences.edit()
        val keyName = key.name
        when (value) {
            is String -> editor.putString(keyName, value)
            is Int -> editor.putInt(keyName, value)
            is Long -> editor.putLong(keyName, value)
            is Double -> editor.putFloat(keyName, value.toFloat())
            is Float -> editor.putFloat(keyName, value)
            is Boolean -> editor.putBoolean(keyName, value)
            is Set<*> -> if (value.all { it is String }) {
                editor.putStringSet(keyName, value as Set<String>)
            }
            else -> {
//                // 对于复杂对象，使用JSON序列化存储为字符串
//                val jsonValue = try {
//                    json.encodeToString(value)
//                } catch (e: Exception) {
//                    // 如果序列化失败，存储为字符串表示
//                    value.toString()
//                }
//                editor.putString(keyName, jsonValue)
            }
        }
        
        editor.apply()
    }
    
    actual fun <T, V> get(key: KProperty1<T, *>, defaultValue: V): V {
        val keyName = key.name
        return when (defaultValue) {
            is String -> sharedPreferences.getString(keyName, defaultValue) as V
            is Int -> sharedPreferences.getInt(keyName, defaultValue) as V
            is Long -> sharedPreferences.getLong(keyName, defaultValue) as V
            is Double -> sharedPreferences.getFloat(keyName, defaultValue.toFloat()) as V
            is Float -> sharedPreferences.getFloat(keyName, defaultValue) as V
            is Boolean -> sharedPreferences.getBoolean(keyName, defaultValue) as V
            is Set<*> -> sharedPreferences.getStringSet(keyName, defaultValue as Set<String>) as V
            else -> {
                sharedPreferences.getString(keyName, defaultValue as String?) as V
//                // 对于复杂对象，从JSON字符串反序列化
//                val jsonString = sharedPreferences.getString(keyName, null)
//                if (jsonString != null && jsonString.isNotEmpty()) {
//                    try {
//                        json.decodeFromString(jsonString)
//                    } catch (e: Exception) {
//                        defaultValue
//                    }
//                } else {
//                    defaultValue
//                }
            }
        }
    }
}
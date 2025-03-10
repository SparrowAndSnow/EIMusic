package com.eimsound.eimusic.data

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

expect class Storage constructor() {
    fun <T,V> save(key: KMutableProperty1<T, V>, value: V)
    fun <T,V> get(key: KProperty1<T, *>, defaultValue: V): V
}

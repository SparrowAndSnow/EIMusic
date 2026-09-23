package com.eimsound.eimusic.serialization

import kotlinx.serialization.json.Json


val Json = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
    serializersModule = module
}
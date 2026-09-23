package com.eimsound.eimusic.server

import com.eimsound.eimusic.serialization.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json)
    }
}


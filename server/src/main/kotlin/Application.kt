package com.eimsound.eimusic.server

import com.eimsound.eimusic.server.admin.configureAdminSessions
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    EngineMain
        .main(args)
}

fun Application.module() {
    configureFrameworks()
    configureSerialization()
    configureSecurity()
    configureAdminSessions()
    configureSockets()
    configureRouting()
}

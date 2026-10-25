package com.eimsound.eimusic.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.eimsound.eimusic.db.EIMusicDatabase
import java.io.File

actual fun createEIMusicDatabase(): EIMusicDatabase {
    val dir = File(System.getProperty("user.home"), ".config/eimusic").apply { mkdirs() }
    val file = File(dir, "eimusic.db")
    val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")

    val version = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 0,
    ).value

    val schema = EIMusicDatabase.Schema
    when {
        version == 0L -> {
            schema.create(driver)
            driver.execute(null, "PRAGMA user_version = ${schema.version}", 0)
        }
        version < schema.version -> {
            schema.migrate(driver, version, schema.version)
            driver.execute(null, "PRAGMA user_version = ${schema.version}", 0)
        }
    }
    return EIMusicDatabase(driver)
}

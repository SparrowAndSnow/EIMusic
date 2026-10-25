package com.eimsound.eimusic.data

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.eimsound.eimusic.EIMusicApplication
import com.eimsound.eimusic.db.EIMusicDatabase

actual fun createEIMusicDatabase(): EIMusicDatabase {
    val driver = AndroidSqliteDriver(
        schema = EIMusicDatabase.Schema,
        context = EIMusicApplication.instance,
        name = "eimusic.db",
    )
    return EIMusicDatabase(driver)
}

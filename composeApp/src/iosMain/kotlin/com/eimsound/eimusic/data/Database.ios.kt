package com.eimsound.eimusic.data

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.eimsound.eimusic.db.EIMusicDatabase
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory

actual fun createEIMusicDatabase(): EIMusicDatabase {
    val dir = "${NSHomeDirectory()}/.config/eimusic"
    NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
    val driver = NativeSqliteDriver(
        schema = EIMusicDatabase.Schema,
        name = "eimusic.db",
        onConfiguration = { config ->
            config.copy(extendedConfig = config.extendedConfig.copy(basePath = dir))
        },
    )
    return EIMusicDatabase(driver)
}

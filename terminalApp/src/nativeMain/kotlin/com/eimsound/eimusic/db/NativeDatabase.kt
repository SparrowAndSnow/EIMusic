package com.eimsound.eimusic.db

import com.eimsound.eimusic.defaultConfigDir
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

fun createEIMusicDatabase(): EIMusicDatabase {
    val dir = defaultConfigDir()
    SystemFileSystem.createDirectories(Path(dir))
    val driver = NativeSqliteDriver(EIMusicDatabase.Schema, "$dir/eimusic.db")
    return EIMusicDatabase(driver)
}

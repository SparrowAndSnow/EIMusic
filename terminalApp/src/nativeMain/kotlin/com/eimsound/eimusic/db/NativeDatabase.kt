package com.eimsound.eimusic.db

import com.eimsound.eimusic.defaultConfigDir
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

fun createEIMusicDatabase(): EIMusicDatabase {
    val dir = defaultConfigDir()
    com.eimsound.eimusic.util.TuiLogger.log("SQLite: opening $dir/eimusic.db")
    SystemFileSystem.createDirectories(Path(dir))
    val driver = NativeSqliteDriver(
        schema = EIMusicDatabase.Schema,
        name = "eimusic.db",
        onConfiguration = { config ->
            config.copy(extendedConfig = config.extendedConfig.copy(basePath = dir))
        },
    )
    return EIMusicDatabase(driver).also {
        // 强制打开连接并执行 schema，避免只有首次查询才落文件。
        it.trackQueries.selectAllTracks().executeAsList()
        com.eimsound.eimusic.util.TuiLogger.log("SQLite: ready")
    }
}

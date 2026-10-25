package com.eimsound.eimusic.data

import com.eimsound.eimusic.db.EIMusicDatabase

/** 创建 composeApp 使用的 SQLDelight 数据库；各平台提供实际路径/driver。 */
expect fun createEIMusicDatabase(): EIMusicDatabase

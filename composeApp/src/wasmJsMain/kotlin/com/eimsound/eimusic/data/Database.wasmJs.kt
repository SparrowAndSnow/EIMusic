package com.eimsound.eimusic.data

import com.eimsound.eimusic.db.EIMusicDatabase

actual fun createEIMusicDatabase(): EIMusicDatabase =
    error("SQLite is not available on wasmJs; composeApp wasm keeps its JSON storage fallback")

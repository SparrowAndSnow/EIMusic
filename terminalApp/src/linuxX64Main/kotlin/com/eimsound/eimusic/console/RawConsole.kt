package com.eimsound.eimusic.console

actual fun restoreConsoleMode() {
    // No-op on Linux — terminal raw mode is not affected by miniaudio
}

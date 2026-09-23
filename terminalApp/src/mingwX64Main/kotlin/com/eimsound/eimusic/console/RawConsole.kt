package com.eimsound.eimusic.console

import kotlinx.cinterop.*
import platform.windows.*

@OptIn(ExperimentalForeignApi::class)
actual fun restoreConsoleMode() {
    // STD_INPUT_HANDLE = (DWORD)-10 = 0xFFFFFFF6
    val handle = GetStdHandle(0xFFFFFFF6u)
    if (handle == null) return

    memScoped {
        val mode = alloc<UIntVar>()
        // GetConsoleMode returns BOOL (0 = fail, nonzero = success)
        if (GetConsoleMode(handle, mode.ptr) != 0) {
            // Remove cooked-mode flags:
            //   ENABLE_PROCESSED_INPUT(0x0001) | ENABLE_LINE_INPUT(0x0002) | ENABLE_ECHO_INPUT(0x0004)
            // Add raw-mode flags:
            //   ENABLE_WINDOW_INPUT(0x0008) | ENABLE_MOUSE_INPUT(0x0010) | ENABLE_EXTENDED_FLAGS(0x0080)
            val cooked = 0x0001u or 0x0002u or 0x0004u
            val raw = 0x0008u or 0x0010u or 0x0080u
            val newMode = (mode.value and cooked.inv()) or raw
            SetConsoleMode(handle, newMode)
        }
    }
}

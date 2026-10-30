package com.eimsound.eimusic

import platform.posix.setenv

actual fun setEnv(name: String, value: String) {
    setenv(name, value, 1)
}

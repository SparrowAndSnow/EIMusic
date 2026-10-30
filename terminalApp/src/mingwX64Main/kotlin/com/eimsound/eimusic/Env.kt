package com.eimsound.eimusic

import platform.posix._putenv_s

actual fun setEnv(name: String, value: String) {
    // Windows CRT 没有 setenv；_putenv_s 会复制字符串并更新环境。
    _putenv_s(name, value)
}

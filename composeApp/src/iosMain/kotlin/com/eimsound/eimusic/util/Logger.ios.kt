package com.eimsound.eimusic.util

import platform.Foundation.NSLog

actual fun log(message: String) {
    NSLog("%@", message)
}

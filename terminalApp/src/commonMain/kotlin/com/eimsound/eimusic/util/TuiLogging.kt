package com.eimsound.eimusic.util

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

private const val LOG_FILE = "/tmp/eimusic.log"

/**
 * 配置 [TuiLogger] 落盘并写入启动提示。
 *
 * 终端 UI 自身不展示这些提示，日志同时可由 LogViewer 屏幕查看；
 * 写文件失败（例如 Windows 无 /tmp）会被静默忽略，不影响应用启动。
 */
fun initTuiLogging() {
    TuiLogger.fileWriter = { line ->
        runCatching {
            SystemFileSystem.sink(Path(LOG_FILE), append = true).buffered().use { sink ->
                sink.write((line + "\n").encodeToByteArray())
                sink.flush()
            }
        }
    }
    TuiLogger.log("App started")
    TuiLogger.log("Tip: 终端图标需要 Nerd Font 支持 (https://www.nerdfonts.com/font-downloads)")
    TuiLogger.log("      安装后设为终端字体即可显示  等控制图标")
    TuiLogger.log("Tip: 专辑封面需要 chafa 支持 (https://hpjansson.org/chafa/)")
    TuiLogger.log("      Arch: sudo pacman -S chafa")
    TuiLogger.log("      Debian/Ubuntu: sudo apt install chafa")
    TuiLogger.log("      Fedora: sudo dnf install chafa")
    TuiLogger.log("      macOS: brew install chafa")
    TuiLogger.log("      Windows: winget install chafa")
}

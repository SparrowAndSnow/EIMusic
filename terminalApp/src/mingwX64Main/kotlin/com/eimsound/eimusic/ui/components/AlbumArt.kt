@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.eimsound.eimusic.ui.components

import com.eimsound.eimusic.getEnvVar
import com.eimsound.eimusic.util.TuiLogger
import com.jakewharton.mosaic.ui.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.posix.*

/**
 * 检测 chafa 是否已安装
 * 通过执行 "chafa --version" 命令来检查
 */
private fun isChafaInstalled(): Boolean {
    return try {
        val result = system("chafa --version >nul 2>&1")
        result == 0 // 返回值为 0 表示命令成功执行
    } catch (e: Exception) {
        false
    }
}

/**
 * 标记 chafa 检测结果，避免重复检查
 */
private var chafaChecked = false
private var chafaAvailable = false

/**
 * 串行化转换：窗口缩放会取消 AlbumArt 的 LaunchedEffect，但 system() 里的 curl/chafa 是阻塞调用，
 * 取消后仍会跑完；并发调用共用同一个临时文件名时，后面的删除会撞上前一个进程的打开句柄。
 */
private val convertLock = Mutex()

/** 每次调用都用独立的临时文件名，避免并发/上一次残留的文件互相冲突。 */
private var tempSequence = 0

/** 直接删文件，不走 cmd：`del` 的报错会打到 Mosaic 的屏幕上，把界面刷乱。 */
private fun deleteQuietly(path: String) {
    runCatching { SystemFileSystem.delete(Path(path), mustExist = false) }
}

internal actual suspend fun convertImage(url: String, cols: Int, rows: Int): List<List<Pair<Color, Char>>> {
    return withContext(Dispatchers.Default) {
        val context = coroutineContext
        convertLock.withLock {
            // 排队期间被取消（又缩放了）就直接放弃，不再启动一轮 curl/chafa。
            if (!context.isActive) {
                return@withLock emptyList()
            }

            // 首次调用时检测 chafa 是否可用
            if (!chafaChecked) {
                chafaAvailable = isChafaInstalled()
                chafaChecked = true

                if (!chafaAvailable) {
                    TuiLogger.log("AlbumArt: chafa 未安装，无法渲染字符图片")
                } else {
                    TuiLogger.log("AlbumArt: chafa 检测成功，可以渲染字符图片")
                }
            }

            // 如果 chafa 不可用，直接返回空列表
            if (!chafaAvailable) {
                return@withLock emptyList()
            }

            // Windows 临时文件路径
            val tempDir = getEnvVar("TEMP") ?: "C:\\Windows\\Temp"
            val stamp = ++tempSequence
            val tmpImg = "$tempDir\\eimusic_art_${url.hashCode()}_$stamp.img"
            val tmpOut = "$tempDir\\eimusic_art_${url.hashCode()}_$stamp.txt"

            try {
                // 使用 curl 下载图片（Windows 10+ 自带 curl），所有输出都丢弃，不要污染 TUI
                system("curl -sL -o \"$tmpImg\" \"$url\" >nul 2>&1")

                // 使用 chafa 转换图片为字符画
                // -f symbols: 使用符号模式
                // --symbols block: 使用块状符号
                // -c full: 使用全彩模式
                // --fg-only: 只输出前景色
                // -s: 指定尺寸
                system("chafa -f symbols --symbols block -c full --fg-only -s ${cols}x${rows} \"$tmpImg\" 2>nul > \"$tmpOut\"")

                // 读取转换结果
                val lines = mutableListOf<List<Pair<Color, Char>>>()
                val content = runCatching {
                    SystemFileSystem.source(Path(tmpOut)).buffered().use { src ->
                        val bytes = mutableListOf<Byte>()
                        while (!src.exhausted()) bytes.add(src.readByte())
                        bytes.toByteArray().decodeToString()
                    }
                }.getOrNull() ?: return@withLock lines

                // 解析每一行
                content.lines().forEach { line ->
                    val trimmed = line.trimEnd('\n', '\r')
                    if (trimmed.isNotEmpty()) {
                        val parsed = parseChafaLine(trimmed)
                        if (parsed.isNotEmpty()) lines.add(parsed)
                    }
                }

                lines
            } catch (e: Exception) {
                TuiLogger.log("AlbumArt: ${e.message}")
                emptyList()
            } finally {
                // 无论成功、失败还是提前返回都清掉本次的两个临时文件。
                deleteQuietly(tmpImg)
                deleteQuietly(tmpOut)
            }
        }
    }
}

/**
 * 解析 chafa 输出的 ANSI 转义序列
 * 格式: ESC[38;2;R;G;Bm 表示 RGB 颜色
 */
private fun parseChafaLine(line: String): List<Pair<Color, Char>> {
    val result = mutableListOf<Pair<Color, Char>>()
    val defaultColor = Color(red = 100, green = 100, blue = 100)
    var curColor = defaultColor
    var i = 0

    while (i < line.length) {
        if (line[i] == '\u001b') { // ESC 字符
            i++
            if (i < line.length && line[i] == '[') {
                i++
                val seq = StringBuilder()
                while (i < line.length && line[i] != 'm') {
                    seq.append(line[i])
                    i++
                }
                i++ // 跳过 'm'

                val parts = seq.toString().split(';')
                when {
                    // 重置颜色
                    parts.size == 1 && parts[0] == "0" -> {
                        curColor = defaultColor
                    }
                    // RGB 前景色: ESC[38;2;R;G;Bm
                    parts.size >= 5 && parts[0] == "38" && parts[1] == "2" -> {
                        curColor = Color(
                            red = (parts[2].toIntOrNull() ?: 0).coerceIn(0, 255),
                            green = (parts[3].toIntOrNull() ?: 0).coerceIn(0, 255),
                            blue = (parts[4].toIntOrNull() ?: 0).coerceIn(0, 255)
                        )
                    }
                }
            }
        } else {
            result.add(curColor to line[i])
            i++
        }
    }
    return result
}

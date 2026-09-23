@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.eimsound.eimusic.ui.components

import com.eimsound.eimusic.getEnvVar
import com.eimsound.eimusic.util.TuiLogger
import com.jakewharton.mosaic.ui.Color
import kotlinx.coroutines.Dispatchers
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

internal actual suspend fun convertImage(url: String, cols: Int, rows: Int): List<List<Pair<Color, Char>>> {
    return withContext(Dispatchers.Default) {
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
            return@withContext emptyList()
        }
        
        try {
            // Windows 临时文件路径
            val tempDir = getEnvVar("TEMP") ?: "C:\\Windows\\Temp"
            val tmpImg = "$tempDir\\eimusic_art_${url.hashCode()}.img"
            val tmpOut = "$tempDir\\eimusic_art_${url.hashCode()}.txt"
            
            // 使用 curl 下载图片（Windows 10+ 自带 curl）
            system("curl -sL -o \"$tmpImg\" \"$url\" 2>nul")
            
            // 使用 chafa 转换图片为字符画
            // -f symbols: 使用符号模式
            // --symbols block: 使用块状符号
            // -c full: 使用全彩模式
            // --fg-only: 只输出前景色
            // -s: 指定尺寸
            system("chafa -f symbols --symbols block -c full --fg-only -s ${cols}x${rows} \"$tmpImg\" 2>nul > \"$tmpOut\"")
            
            // 清理临时图片文件
            system("del /f /q \"$tmpImg\" 2>nul")
            
            // 读取转换结果
            val lines = mutableListOf<List<Pair<Color, Char>>>()
            val content = runCatching {
                SystemFileSystem.source(Path(tmpOut)).buffered().use { src ->
                    val bytes = mutableListOf<Byte>()
                    while (!src.exhausted()) bytes.add(src.readByte())
                    bytes.toByteArray().decodeToString()
                }
            }.getOrNull() ?: return@withContext lines
            
            // 解析每一行
            content.lines().forEach { line ->
                val trimmed = line.trimEnd('\n', '\r')
                if (trimmed.isNotEmpty()) {
                    val parsed = parseChafaLine(trimmed)
                    if (parsed.isNotEmpty()) lines.add(parsed)
                }
            }
            
            // 清理临时输出文件
            system("del /f /q \"$tmpOut\" 2>nul")
            lines
        } catch (e: Exception) {
            TuiLogger.log("AlbumArt: ${e.message}")
            emptyList()
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

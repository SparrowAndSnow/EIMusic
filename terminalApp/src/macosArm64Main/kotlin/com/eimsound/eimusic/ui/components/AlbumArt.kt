@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.eimsound.eimusic.ui.components

import com.eimsound.eimusic.util.TuiLogger
import com.jakewharton.mosaic.ui.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.posix.*

private var chafaChecked = false
private var chafaAvailable = false

private fun isChafaInstalled(): Boolean {
    return try {
        system("chafa --version >/dev/null 2>&1") == 0
    } catch (_: Exception) { false }
}

internal actual suspend fun convertImage(url: String, cols: Int, rows: Int): List<List<Pair<Color, Char>>> {
    return withContext(Dispatchers.Default) {
        if (!chafaChecked) {
            chafaAvailable = isChafaInstalled()
            chafaChecked = true
            if (!chafaAvailable) {
                TuiLogger.log("AlbumArt: chafa 未安装，无法渲染字符图片")
            } else {
                TuiLogger.log("AlbumArt: chafa 检测成功，可以渲染字符图片")
            }
        }
        if (!chafaAvailable) return@withContext emptyList()

        try {
            val tmpImg = "/tmp/eimusic_art_${url.hashCode()}.img"
            val tmpOut = "/tmp/eimusic_art_${url.hashCode()}.txt"
            system("timeout 8 curl -sL -o \"$tmpImg\" \"$url\" 2>/dev/null")
            system("timeout 8 chafa -f symbols --symbols block -c full --fg-only -s ${cols}x${rows} \"$tmpImg\" 2>/dev/null > \"$tmpOut\"")
            system("rm -f \"$tmpImg\" 2>/dev/null")
            val lines = mutableListOf<List<Pair<Color, Char>>>()
            val content = runCatching {
                SystemFileSystem.source(Path(tmpOut)).buffered().use { src ->
                    val bytes = mutableListOf<Byte>()
                    while (!src.exhausted()) bytes.add(src.readByte())
                    bytes.toByteArray().decodeToString()
                }
            }.getOrNull() ?: return@withContext lines
            content.lines().forEach { line ->
                val trimmed = line.trimEnd('\n', '\r')
                if (trimmed.isNotEmpty()) { val parsed = parseChafaLine(trimmed); if (parsed.isNotEmpty()) lines.add(parsed) }
            }
            system("rm -f \"$tmpOut\"")
            lines
        } catch (e: Exception) { TuiLogger.log("AlbumArt: ${e.message}"); emptyList() }
    }
}

private fun parseChafaLine(line: String): List<Pair<Color, Char>> {
    val result = mutableListOf<Pair<Color, Char>>()
    val defaultColor = Color(red = 100, green = 100, blue = 100)
    var curColor = defaultColor; var i = 0
    while (i < line.length) {
        if (line[i] == '') { i++; if (i < line.length && line[i] == '[') { i++; val seq = StringBuilder(); while (i < line.length && line[i] != 'm') { seq.append(line[i]); i++ }; i++; val parts = seq.toString().split(';'); if (parts.size == 1 && parts[0] == "0") curColor = defaultColor; else if (parts.size >= 5 && parts[0] == "38" && parts[1] == "2") { curColor = Color(red = (parts[2].toIntOrNull() ?: 0).coerceIn(0, 255), green = (parts[3].toIntOrNull() ?: 0).coerceIn(0, 255), blue = (parts[4].toIntOrNull() ?: 0).coerceIn(0, 255)) } } }
        else { result.add(curColor to line[i]); i++ }
    }
    return result
}

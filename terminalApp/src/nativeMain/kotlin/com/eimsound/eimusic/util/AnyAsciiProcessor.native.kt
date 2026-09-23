package com.eimsound.eimusic.util

import anyascii.anyascii_render
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString

class AnyAsciiProcessor : TextProcessor {

    override fun process(text: String): String {
        if (text.all { it.code < 0x80 }) return text
        return buildString(text.length * 2) {
            for (c in text) {
                append(transliterateCodePoint(c.code) ?: FALLBACK[c] ?: c.toString())
            }
        }
    }

    companion object {
        /**
         * Manual mappings for characters that [anyascii] may not transliterate.
         */
        private val FALLBACK = mapOf(
            'ー' to "-",  // Katakana-Hiragana Prolonged Sound Mark
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun transliterateCodePoint(codePoint: Int): String? {
    return memScoped {
        val buf = allocArray<ByteVar>(16)
        val len = anyascii_render(codePoint.toUInt(), buf, 16uL)
        if (len == 0uL) return@memScoped null
        buf.toKString()
    }
}

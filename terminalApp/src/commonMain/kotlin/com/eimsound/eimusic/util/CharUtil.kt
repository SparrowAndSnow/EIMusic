package com.eimsound.eimusic.util

import com.jakewharton.mosaic.text.AnnotatedString

/** Returns [n] spaces. Use inside buildAnnotatedString: append(spacer(2)). */
val Int.spacer get() = " ".repeat(this)

fun AnnotatedString.Builder.spacer(n: Int) = append(n.spacer)

/** Returns the terminal column width of a single character (CJK = 2, ASCII = 1). */
fun charWidth(c: Char): Int = when (c.code) {
    in 0x1100..0x11FF -> 2; in 0x2E80..0xA4CF -> 2; in 0xAC00..0xD7AF -> 2
    in 0xF900..0xFAFF -> 2; in 0xFE10..0xFE6F -> 2; in 0xFF01..0xFF60 -> 2
    in 0x2F800..0x2FA1F -> 2; else -> 1
}

/** Returns the visual width of [s] in a terminal (CJK = 2 columns). */
fun visualWidth(s: String): Int = s.sumOf { charWidth(it) }

/** Preprocesses text through the installed Renderer processors. */
fun String.render(): String = Renderer.render(this)

/** Truncated string result with its visual width. */
data class TruncateResult(val text: String, val width: Int)

/**
 * Truncates this string to at most [maxWidth] visual columns.
 * Appends "…" when truncated. Returns the original string if it fits.
 */
fun String.truncate(maxWidth: Int): String {
    if (maxWidth <= 0) return ""
    val w = visualWidth(this)
    if (w <= maxWidth) return this
    return truncateImpl(this, maxWidth)
}

/**
 * Like [truncate] but also returns the visual width of the result.
 * Useful for layout calculations (e.g. HeaderBar).
 */
fun String.truncated(maxWidth: Int): TruncateResult {
    if (maxWidth <= 0) return TruncateResult("", 0)
    val w = visualWidth(this)
    if (w <= maxWidth) return TruncateResult(this, w)
    val t = truncateImpl(this, maxWidth)
    return TruncateResult(t, maxWidth)
}

private fun truncateImpl(s: String, max: Int): String {
    if (max == 1) return "…"
    var w = 0
    val sb = StringBuilder()
    for (c in s) {
        val cw = charWidth(c)
        if (w + cw > max - 1) break
        sb.append(c)
        w += cw
    }
    return sb.append('…').toString()
}

/**
 * Pads this string to at least [width] terminal visual columns by appending spaces.
 * Unlike [String.padEnd], this uses [visualWidth] so CJK characters count as 2 columns.
 */
fun String.padEndVisual(width: Int): String {
    val current = visualWidth(this)
    if (current >= width) return this
    return this + " ".repeat(width - current)
}

/**
 * Pads this string to at least [width] terminal visual columns by prepending spaces.
 * Unlike [String.padStart], this uses [visualWidth] so CJK characters count as 2 columns.
 */
fun String.padStartVisual(width: Int): String {
    val current = visualWidth(this)
    if (current >= width) return this
    return " ".repeat(width - current) + this
}

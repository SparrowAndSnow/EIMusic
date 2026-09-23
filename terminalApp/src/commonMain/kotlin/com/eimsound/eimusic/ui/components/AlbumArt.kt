package com.eimsound.eimusic.ui.components

import androidx.compose.runtime.*
import com.eimsound.eimusic.ui.theme.LocalTuiColors
import com.jakewharton.mosaic.layout.background
import com.jakewharton.mosaic.layout.fillMaxWidth
import com.jakewharton.mosaic.layout.width
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Alignment
import com.jakewharton.mosaic.ui.Box
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text

/** 全局图片转换缓存，避免反复调用 chafa。key 为 "url|cols|rows"。 */
internal object ArtCache {
    private val cache = mutableMapOf<String, List<List<Pair<Color, Char>>>>()
    fun get(key: String) = cache[key]
    fun set(key: String, art: List<List<Pair<Color, Char>>>) { cache[key] = art }
    fun key(url: String, cols: Int, rows: Int) = "$url|$cols|$rows"
}

/**
 * Expects image URL → colored character grid conversion.
 * Actual is provided per-platform (nativeMain uses chafa).
 */
internal expect suspend fun convertImage(
    url: String, cols: Int, rows: Int
): List<List<Pair<Color, Char>>>

@Composable
fun AlbumArt(
    imageUrl: String?,
    width: Int,
    height: Int,
) {
    val palette = LocalTuiColors.current
    var art by remember(imageUrl, width, height) { mutableStateOf<List<List<Pair<Color, Char>>>>(emptyList()) }
    var loading by remember(imageUrl) { mutableStateOf(imageUrl != null) }

    LaunchedEffect(imageUrl, width, height) {
        if (imageUrl.isNullOrBlank()) {
            loading = false; return@LaunchedEffect
        }
        val cacheKey = ArtCache.key(imageUrl, width, height)
        val cached = ArtCache.get(cacheKey)
        if (cached != null) {
            art = cached
            loading = false
        } else {
            art = convertImage(imageUrl, width, height)
            ArtCache.set(cacheKey, art)
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (loading || art.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "♫ Album Art ♫",
                    modifier = Modifier.width(width).background(palette.surfaceBg),
                    color = palette.textDimFg
                )
            }
            (2..height).forEach { Text(" ".repeat(width), color = palette.textDimFg) }
        } else {
            art.take(height).forEach { row ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        buildAnnotatedString {
                            val n = row.size.coerceAtMost(width)
                            (0 until n).forEach { i ->
                                val (color, char) = row[i]
                                withStyle(SpanStyle(color = color)) { append(char.toString()) }
                            }
                            if (n < width) {
                                withStyle(SpanStyle(color = palette.textDimFg)) { append(" ".repeat(width - n)) }
                            }
                        },
                        color = palette.textFg,
                    )
                }
            }
            (art.size.coerceAtMost(height) until height).forEach { Text(" ".repeat(width), color = palette.textDimFg) }
        }
    }
}

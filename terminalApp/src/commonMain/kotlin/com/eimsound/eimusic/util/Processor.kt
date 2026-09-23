package com.eimsound.eimusic.util

fun interface TextProcessor {
    fun process(text: String): String
}

object Renderer {
    private val processors = mutableListOf<TextProcessor>()

    fun install(p: TextProcessor) {
        processors.add(p)
    }

    fun clear() {
        processors.clear()
    }

    fun render(text: String): String {
        if (text.all { it.code < 0x80 }) return text
        if (processors.isEmpty()) return text
        var r = text
        for (p in processors) r = p.process(r)
        return r
    }
}

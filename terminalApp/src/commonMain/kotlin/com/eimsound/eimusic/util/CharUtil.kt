package com.eimsound.eimusic.util

import com.jakewharton.mosaic.text.AnnotatedString

/** Returns [n] spaces. Use inside buildAnnotatedString: append(spacer(2)). */
val Int.spacer get() = " ".repeat(this)

fun AnnotatedString.Builder.spacer(n: Int) = append(n.spacer)

/** Preprocesses text through the installed Renderer processors. */
fun String.render(): String = Renderer.render(this)

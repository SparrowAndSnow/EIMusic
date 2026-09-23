package com.eimsound.eimusic.audio

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
expect class StreamBuffer() {
    fun append(data: ByteArray)
    fun close()
    fun markError(msg: String)
    fun read(pBufferOut: CPointer<*>?, bytesToRead: Long, pBytesRead: CPointer<*>?)
    fun seek(byteOffset: Long, origin: Int): Int
    fun available(): Int
    fun isClosed(): Boolean
    fun dispose()
}

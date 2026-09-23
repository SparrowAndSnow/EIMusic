package com.eimsound.eimusic.audio

import com.eimsound.eimusic.util.TuiLogger
import kotlinx.cinterop.*
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.min
import platform.windows.Sleep
import kotlin.concurrent.atomics.AtomicBoolean

/**
 * Windows implementation of [StreamBuffer].
 * Uses Kotlin/Native [Synchronized] for mutual exclusion and Win32 [Sleep] for busy-wait.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class)
actual class StreamBuffer {
    private var buffer = ByteArray(INITIAL_CAPACITY)
    private val writePos = AtomicInt(0)
    private val readPos = AtomicInt(0)
    private val closed = AtomicBoolean(false) // 0 = false, 1 = true
    private var error: String? = null

    actual fun append(data: ByteArray) {
        ensureCapacity(writePos.load() + data.size)
        data.copyInto(buffer, writePos.load())
        writePos.store(writePos.load() + data.size)
    }

    actual fun close() {
        closed.store(true)
    }

    actual fun markError(msg: String) {
        TuiLogger.log("StreamBuffer error: $msg")
        error = msg
        closed.store(true)
    }

    actual fun read(pBufferOut: CPointer<*>?, bytesToRead: Long, pBytesRead: CPointer<*>?) {
        // Busy-wait: yield while no data available and not closed
        while (readPos.load() >= writePos.load() && !closed.load()) {
            Sleep(1u)
        }

        val available = min(bytesToRead.toInt(), writePos.load() - readPos.load())
        if (available > 0 && pBufferOut != null) {
            val dst = pBufferOut.reinterpret<ByteVar>()
            for (i in 0 until available) {
                dst[i] = buffer[readPos.load() + i]
            }
            readPos.store(readPos.load() + available)
        }
        if (pBytesRead != null) {
            val ptr = pBytesRead.reinterpret<ULongVar>()
            ptr.pointed.value = available.toULong()
        }
    }

    actual fun seek(byteOffset: Long, origin: Int): Int {
        val newPos = when (origin) {
            0 -> byteOffset
            1 -> readPos.load().toLong() + byteOffset
            2 -> writePos.load().toLong() + byteOffset
            else -> return -1
        }
        if (newPos < 0 || newPos > writePos.load()) return -1
        readPos.store(newPos.toInt())
        return 0
    }

    actual fun available(): Int = writePos.load() - readPos.load()

    actual fun isClosed(): Boolean = !closed.load()

    actual fun dispose() {
        close()
    }

    private fun ensureCapacity(minCapacity: Int) {
        if (minCapacity > buffer.size) {
            var newSize = buffer.size * 2
            while (newSize < minCapacity) newSize *= 2
            buffer = buffer.copyOf(newSize)
        }
    }

    companion object {
        private const val INITIAL_CAPACITY = 65536 // 64KB
    }
}

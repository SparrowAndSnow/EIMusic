package com.eimsound.eimusic.audio

import com.eimsound.eimusic.util.TuiLogger
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
actual class StreamBuffer {
    private var buffer = ByteArray(INITIAL_CAPACITY)
    private var writePos = 0
    private var readPos = 0
    private var closed = false
    private var error: String? = null

    private val mutex = nativeHeap.alloc<pthread_mutex_t>()
    private val cond = nativeHeap.alloc<pthread_cond_t>()

    init {
        pthread_mutex_init(mutex.ptr, null)
        pthread_cond_init(cond.ptr, null)
    }

    actual fun append(data: ByteArray) {
        pthread_mutex_lock(mutex.ptr)
        try {
            ensureCapacity(writePos + data.size)
            data.copyInto(buffer, writePos)
            writePos += data.size
            pthread_cond_broadcast(cond.ptr)
        } finally {
            pthread_mutex_unlock(mutex.ptr)
        }
    }

    actual fun close() {
        pthread_mutex_lock(mutex.ptr)
        closed = true
        pthread_cond_broadcast(cond.ptr)
        pthread_mutex_unlock(mutex.ptr)
    }

    actual fun markError(msg: String) {
        TuiLogger.log("StreamBuffer error: $msg")
        pthread_mutex_lock(mutex.ptr)
        error = msg
        closed = true
        pthread_cond_broadcast(cond.ptr)
        pthread_mutex_unlock(mutex.ptr)
    }

    actual fun read(pBufferOut: CPointer<*>?, bytesToRead: Long, pBytesRead: CPointer<*>?) {
        pthread_mutex_lock(mutex.ptr)
        try {
            while (readPos >= writePos && !closed) {
                pthread_cond_wait(cond.ptr, mutex.ptr)
            }
            val available = min(bytesToRead.toInt(), writePos - readPos)
            if (available > 0 && pBufferOut != null) {
                val dst = pBufferOut.reinterpret<ByteVar>()
                for (i in 0 until available) {
                    dst[i] = buffer[readPos + i]
                }
                readPos += available
            }
            if (pBytesRead != null) {
                val ptr = pBytesRead.reinterpret<ULongVar>()
                ptr.pointed.value = available.toULong()
            }
        } finally {
            pthread_mutex_unlock(mutex.ptr)
        }
    }

    actual fun seek(byteOffset: Long, origin: Int): Int {
        pthread_mutex_lock(mutex.ptr)
        try {
            val newPos = when (origin) {
                0 -> byteOffset
                1 -> readPos.toLong() + byteOffset
                2 -> writePos.toLong() + byteOffset
                else -> return -1
            }
            if (newPos < 0 || newPos > writePos) return -1
            readPos = newPos.toInt()
            return 0
        } finally {
            pthread_mutex_unlock(mutex.ptr)
        }
    }

    actual fun available(): Int {
        pthread_mutex_lock(mutex.ptr)
        try { return writePos - readPos }
        finally { pthread_mutex_unlock(mutex.ptr) }
    }

    actual fun isClosed(): Boolean {
        pthread_mutex_lock(mutex.ptr)
        try { return closed }
        finally { pthread_mutex_unlock(mutex.ptr) }
    }

    actual fun dispose() {
        close()
        pthread_mutex_destroy(mutex.ptr)
        pthread_cond_destroy(cond.ptr)
        nativeHeap.free(mutex)
        nativeHeap.free(cond)
    }

    private fun ensureCapacity(minCapacity: Int) {
        if (minCapacity > buffer.size) {
            var newSize = buffer.size * 2
            while (newSize < minCapacity) newSize *= 2
            buffer = buffer.copyOf(newSize)
        }
    }

    companion object {
        private const val INITIAL_CAPACITY = 65536
    }
}

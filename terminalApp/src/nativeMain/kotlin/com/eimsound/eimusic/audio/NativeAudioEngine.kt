package com.eimsound.eimusic.audio

import com.eimsound.eimusic.console.restoreConsoleMode
import com.eimsound.eimusic.util.TuiLogger
import kotlinx.cinterop.*
import miniaudio.*

@OptIn(ExperimentalForeignApi::class)
class NativeAudioEngine {
    private val enginePtr: CPointer<ma_engine>
    private val soundPtr: CPointer<ma_sound>
    private var soundLoaded = false

    /** 流式播放相关状态 */
    private var decoderPtr: CPointer<ma_decoder>? = null

    companion object {
        private const val FLAG_STREAM = 0x00000001u
        private const val FLAG_NO_SPATIALIZATION = 0x00004000u

        /** 当前活跃的 StreamBuffer，供 C 回调访问 */
        private var activeBuffer: StreamBuffer? = null

        private val onReadCallback: ma_decoder_read_proc = staticCFunction { _, pBufferOut, bytesToRead, pBytesRead ->
            val buffer = activeBuffer ?: return@staticCFunction MA_ERROR
            buffer.read(pBufferOut, bytesToRead.toLong(), pBytesRead)
            MA_SUCCESS
        }

        private val onSeekCallback: ma_decoder_seek_proc = staticCFunction { _, byteOffset, origin ->
            val buffer = activeBuffer ?: return@staticCFunction MA_ERROR
            val result = buffer.seek(byteOffset, origin.ordinal)
            if (result != 0) return@staticCFunction MA_NOT_IMPLEMENTED
            MA_SUCCESS
        }
    }

    init {
        enginePtr = nativeHeap.alloc<ma_engine>().ptr
        soundPtr = nativeHeap.alloc<ma_sound>().ptr
        memScoped {
            val config = ma_engine_config_init()
            val result = ma_engine_init(config.ptr, enginePtr)
            if (result != MA_SUCCESS) {
                val msg = "ma_engine_init failed: $result"
                TuiLogger.log(msg)
                throw RuntimeException(msg)
            }
            TuiLogger.log("Engine initialized")
        }
    }

    // ── 文件播放（用于本地文件和缓存文件） ──

    fun openFile(path: String): Long {
        closeCurrent()
        TuiLogger.log("Opening: ${path.take(80)}")
        val result =
            ma_sound_init_from_file(enginePtr, path, FLAG_STREAM or FLAG_NO_SPATIALIZATION, null, null, soundPtr)
        if (result != MA_SUCCESS) {
            val msg = "ma_sound_init_from_file failed: $result"
            TuiLogger.log(msg)
            throw RuntimeException(msg)
        }
        soundLoaded = true
        val dur = duration()
        TuiLogger.log("Sound loaded, duration=${dur}ms")
        restoreConsoleMode()
        return dur
    }

    // ── 流式播放（用于网络音频） ──

    /**
     * 从 [StreamBuffer] 初始化流式播放。
     * 内部创建 ma_decoder（从缓冲区读取编码音频）+ ma_sound（从 decoder 读取 PCM）。
     */
    fun openStream(buffer: StreamBuffer): Long {
        closeCurrent()

        activeBuffer = buffer

        // 创建 decoder
        decoderPtr = nativeHeap.alloc<ma_decoder>().ptr
        memScoped {
            val config = ma_decoder_config_init(ma_format_unknown, 0u, ma_engine_get_sample_rate(enginePtr))
            val result = ma_decoder_init(onReadCallback, onSeekCallback, null, config.ptr, decoderPtr)
            if (result != MA_SUCCESS) {
                decoderPtr = null
                activeBuffer = null
                val msg = "ma_decoder_init failed: $result"
                TuiLogger.log(msg)
                throw RuntimeException(msg)
            }
        }

        // 创建 sound from decoder
        val result = ma_sound_init_from_data_source(enginePtr, decoderPtr, FLAG_NO_SPATIALIZATION, null, soundPtr)
        if (result != MA_SUCCESS) {
            ma_decoder_uninit(decoderPtr)
            decoderPtr = null
            activeBuffer = null
            val msg = "ma_sound_init_from_data_source failed: $result"
            TuiLogger.log(msg)
            throw RuntimeException(msg)
        }

        soundLoaded = true
        val dur = duration()
        TuiLogger.log("Stream initialized, duration=${dur}ms")
        restoreConsoleMode()
        return dur
    }

    // ── 播放控制 ──

    fun play() {
        if (!soundLoaded) return
        ma_sound_start(soundPtr)
        restoreConsoleMode()
    }

    fun pause() {
        if (!soundLoaded) return
        ma_sound_stop(soundPtr)
    }

    fun stop() {
        if (!soundLoaded) return
        ma_sound_stop(soundPtr)
        ma_sound_seek_to_pcm_frame(soundPtr, 0uL)
    }

    fun seek(millis: Long) {
        if (!soundLoaded) return
        val totalFrames = lengthInPcmFrames()
        val totalMs = duration()
        if (totalMs <= 0L || totalFrames <= 0uL) return
        val safeMillis = millis.coerceIn(0L, totalMs)
        val targetFrame = (safeMillis * totalFrames.toLong() / totalMs).toULong()
        ma_sound_seek_to_pcm_frame(soundPtr, targetFrame)
    }

    fun position(): Long {
        if (!soundLoaded) return 0L
        return ma_sound_get_time_in_milliseconds(soundPtr).toLong()
    }

    fun duration(): Long {
        if (!soundLoaded) return 0L
        return memScoped {
            val pcmFrames: ULongVar = alloc()
            ma_sound_get_length_in_pcm_frames(soundPtr, pcmFrames.ptr)
            val frames = pcmFrames.value
            if (frames == 0uL) return 0L
            val sampleRate = ma_engine_get_sample_rate(enginePtr).toLong()
            frames.toLong() * 1000L / sampleRate
        }
    }

    fun setVolume(value: Float) {
        ma_sound_set_volume(soundPtr, value)
    }

    fun isPlaying(): Boolean {
        if (!soundLoaded) return false
        return ma_sound_is_playing(soundPtr) != 0u
    }

    // ── 生命周期 ──

    /** 先停 sound 再销毁 decoder，供切歌/释放复用。 */
    fun closeCurrent() {
        try {
            if (soundLoaded) {
                ma_sound_stop(soundPtr)
                ma_sound_uninit(soundPtr)
                soundLoaded = false
            }
        } catch (_: Exception) {
        }
        cleanupStream()
    }

    fun release() {
        closeCurrent()
        try {
            ma_engine_uninit(enginePtr)
        } catch (_: Exception) {
        }
        nativeHeap.free(enginePtr)
        nativeHeap.free(soundPtr)
    }

    private fun cleanupStream() {
        try {
            if (decoderPtr != null) {
                ma_decoder_uninit(decoderPtr)
                nativeHeap.free(decoderPtr!!)
            }
        } catch (_: Exception) {
        }
        decoderPtr = null
        activeBuffer = null
    }

    private fun lengthInPcmFrames(): ULong {
        return memScoped {
            val frames: ULongVar = alloc()
            ma_sound_get_length_in_pcm_frames(soundPtr, frames.ptr)
            frames.value
        }
    }
}

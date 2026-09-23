package com.eimsound.eimusic.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

object TuiLogger {
    private const val MAX = 200
    private val entries = mutableListOf<String>()
    private val _entriesFlow = MutableStateFlow(emptyList<String>())

    /** 日志列表的 StateFlow，可收集实现实时更新 */
    val entriesFlow: StateFlow<List<String>> = _entriesFlow

    /** 由平台代码注入，用于将日志写入文件 */
    var fileWriter: ((String) -> Unit)? = null

    fun log(msg: String) {
        val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val ts = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
        val line = "[$ts] $msg"
        if (entries.size >= MAX) entries.removeAt(0)
        entries.add(line)
        _entriesFlow.value = entries.toList()
        fileWriter?.invoke(line)
    }

    fun all(): List<String> = entries.toList()
    fun clear() {
        entries.clear()
        _entriesFlow.value = emptyList()
    }
}

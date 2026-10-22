package com.eimsound.eimusic.music

import com.eimsound.eimusic.cycle

/**
 * 与具体播放引擎无关的播放队列 / 导航纯逻辑，
 * 由桌面、终端等各平台媒体控制器复用。
 */

/** 计算下一首选曲（Shuffle/Loop 按当前列表取下一项，RepeatOne 返回当前曲目）。 */
fun PlayerState.nextTrackCandidate(): Track? {
    val list = currentList
    if (list.isEmpty()) return null
    val index = (this.index + 1).cycle(0, list.size - 1)
    val current = list[index]
    return if (playMode is PlayMode.RepeatOne) this.current ?: current else current
}

/** 计算上一首选曲（Shuffle/Loop 按当前列表取上一项，RepeatOne 返回当前曲目）。 */
fun PlayerState.previousTrackCandidate(): Track? {
    val list = currentList
    if (list.isEmpty()) return null
    val index = (this.index - 1).cycle(0, list.size - 1)
    val current = list[index]
    return if (playMode is PlayMode.RepeatOne) this.current ?: current else current
}

/** 在队列中插入（或移动已存在）曲目，并重算当前索引。 */
fun PlayerState.withTrackAdded(media: Track, index: Int): PlayerState {
    val existing = queue.indexOfFirst { it.id == media.id }
    val updated = queue.toMutableList().apply {
        if (existing != -1) removeAt(existing)
        // 调用方常按“插入前的 queue.size”算 index；若 media 已在队列里，
        // 移除后长度会少 1，必须重新收敛到合法范围，否则 add 会越界崩溃。
        add(index.coerceIn(0, size), media)
    }
    return copy(queue = updated, index = recalculatedIndex(updated))
}

/** 移除队列中的曲目，并重算当前索引。 */
fun PlayerState.withTrackRemoved(index: Int): PlayerState {
    if (index !in queue.indices) return this
    val updated = queue.toMutableList().apply { removeAt(index) }
    return copy(queue = updated, index = recalculatedIndex(updated))
}

/** 根据当前曲目在给定队列中重算索引；找不到时回退到 0。 */
fun PlayerState.recalculatedIndex(queue: List<Track>): Int =
    queue.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)

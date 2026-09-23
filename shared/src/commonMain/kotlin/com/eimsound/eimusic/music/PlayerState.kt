package com.eimsound.eimusic.music

import com.eimsound.eimusic.together.PlayState
import kotlin.random.Random

data class PlayerState(
    // 原始播放列表
    val queue: List<Track> = emptyList(),
    val current: Track? = null,
    val index: Int = 0,
    val isLoading: Boolean = false,
    val position: Long = 0,
    val playMode: PlayMode = PlayMode.Loop,
    val volume: Float = 1.0f,
    val isMute: Boolean = false,
    val playState: PlayState = PlayState.STOPPED,
    val duration: Long = 0,
    val bufferProgress: Float = 0.0f,
) {
    private var shuffleList: List<Track>? = null
    private var lastShuffleSeed: Long? = null
    private var lastShuffleIndices: IntArray? = null


    // 当前播放列表
    val currentList: List<Track>
        get() = if (playMode is PlayMode.Shuffle) {
            getShuffleList(playMode)
        } else {
            queue
        }

    private fun getShuffleList(shuffleMode: PlayMode.Shuffle): List<Track> {
        if (shuffleList != null &&
            lastShuffleSeed == shuffleMode.seed &&
            lastShuffleIndices?.contentEquals(shuffleMode.indices) == true
        ) {
            return shuffleList!!
        }
        val result = queue.shuffled(Random(shuffleMode.seed))
//        val result = if (shuffleMode.indices.size == queue.size) {
//            shuffleMode.indices.map { queue[it] }
//        } else {
//            queue
//        }

        shuffleList = result
        lastShuffleSeed = shuffleMode.seed
        lastShuffleIndices = shuffleMode.indices
        return result
    }

    companion object {
        /**
         * 更新随机播放的 indices
         * @param shuffleMode 当前的随机播放模式
         * @param removedIndex 被删除的歌曲索引（-1 表示没有删除）
         * @param addedIndex 添加歌曲的位置（-1 表示没有添加）
         * @param newSize 新的队列大小
         * @return 更新后的 Shuffle 模式
         */
        fun updateShuffleList(
            shuffleMode: PlayMode.Shuffle,
            removedIndex: Int,
            addedIndex: Int,
            newSize: Int
        ): PlayMode.Shuffle {
            val oldIndices = shuffleMode.indices.toList().toMutableList()

            // 如果删除了歌曲，从 indices 中移除对应的索引
            if (removedIndex >= 0 && removedIndex < oldIndices.size) {
                val indexInShuffle = oldIndices.indexOfFirst { it == removedIndex }
                if (indexInShuffle != -1) {
                    oldIndices.removeAt(indexInShuffle)
                }
            }

            // 调整 indices 中的值
            val adjustedIndices = oldIndices.map { idx ->
                when {
                    // 如果在 idx 之前添加了歌曲，idx 需要 +1
                    addedIndex in 0..idx -> idx + 1
                    // 如果在 idx 之前删除了歌曲，idx 需要 -1
                    removedIndex in 0..<idx -> idx - 1
                    else -> idx
                }
            }.toMutableList()

            // 如果添加了歌曲，将新歌曲的索引插入到对应位置
            if (addedIndex >= 0) {
                adjustedIndices.add(addedIndex, newSize - 1)
            }

            return PlayMode.Shuffle(shuffleMode.seed, adjustedIndices.toIntArray())
        }
    }
}
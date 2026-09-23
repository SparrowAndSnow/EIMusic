package com.eimsound.eimusic.music

import com.eimsound.eimusic.music.PlayMode.Loop
import com.eimsound.eimusic.music.PlayMode.RepeatOne
import com.eimsound.eimusic.music.PlayMode.Shuffle
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
sealed interface PlayMode {
    @Serializable
    data class Shuffle(val seed: Long, val indices: IntArray) : PlayMode {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Shuffle

            if (seed != other.seed) return false
            if (!indices.contentEquals(other.indices)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = seed.hashCode()
            result = 31 * result + indices.contentHashCode()
            return result
        }
    }

    @Serializable
    object Loop : PlayMode

    @Serializable
    object RepeatOne : PlayMode

    companion object {
        fun of(name: String): PlayMode = when {
            name.startsWith("Shuffle") -> {
                val parts = name.split(":")
                val seed = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                val indices = parts.getOrNull(2)?.split(",")?.mapNotNull {
                    it.toIntOrNull()
                }?.toIntArray() ?: IntArray(0)
                Shuffle(seed, indices)
            }

            name == "Loop" -> Loop
            name == "RepeatOne" -> RepeatOne
            else -> Loop
        }
    }
}

fun PlayMode.change(onShuffle: () -> Int): PlayMode = when (this) {
    is Shuffle -> Loop
    is Loop -> RepeatOne
    is RepeatOne -> {
        val count = onShuffle()
        val seed = Random.nextLong()
        val indices = generateShuffleIndices(count, seed)
        Shuffle(seed, indices)
    }
}

val PlayMode.name
    get() = when (this) {
        is PlayMode.Loop -> "Loop"

        is PlayMode.Shuffle -> "Shuffle:$seed:${indices.joinToString(",") { it.toString() }}"

        is PlayMode.RepeatOne -> "RepeatOne"
    }

fun generateShuffleIndices(count: Int, seed: Long): IntArray {
    val random = Random(seed)
    val indices = IntArray(count) { it }

    // Fisher-Yates 洗牌算法
    for (i in count - 1 downTo 1) {
        val j = random.nextInt(i + 1)
        val temp = indices[i]
        indices[i] = indices[j]
        indices[j] = temp
    }
    return indices
}

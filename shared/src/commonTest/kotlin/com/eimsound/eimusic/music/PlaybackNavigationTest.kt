package com.eimsound.eimusic.music

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PlaybackNavigationTest {
    private fun track(id: String) = Track(
        album = null,
        artists = null,
        duration = null,
        id = TrackId(TrackSource.LOCAL, id),
        name = id,
        uri = null,
        source = TrackSource.LOCAL,
    )

    @Test
    fun addIntoEmptyQueueClampsInvalidIndex() {
        val media = track("a")
        val state = PlayerState().withTrackAdded(media, 1)
        assertEquals(listOf(media), state.queue)
        assertEquals(0, state.index)
    }

    @Test
    fun addExistingTrackWithPreRemovalSizeDoesNotThrow() {
        // 复现：queue=[a]，调用方用 queue.size=1 作为插入位置，add 前会先移除已存在的 a。
        val media = track("a")
        val state = PlayerState(queue = listOf(media), current = media, index = 0)
            .withTrackAdded(media, 1)
        assertEquals(listOf(media), state.queue)
        assertEquals(0, state.index)
    }

    @Test
    fun addNegativeIndexFallsBackToStart() {
        val media = track("a")
        val state = PlayerState(queue = listOf(track("b"))).withTrackAdded(media, -1)
        assertEquals(listOf(media, track("b")), state.queue)
    }

    @Test
    fun removeInvalidIndexIsNoOp() {
        val state = PlayerState(queue = listOf(track("a")))
        assertSame(state, state.withTrackRemoved(1))
        assertSame(state, state.withTrackRemoved(-1))
    }

    @Test
    fun navigationCandidatesAreNullForEmptyQueue() {
        val state = PlayerState()
        assertNull(state.nextTrackCandidate())
        assertNull(state.previousTrackCandidate())
    }
}

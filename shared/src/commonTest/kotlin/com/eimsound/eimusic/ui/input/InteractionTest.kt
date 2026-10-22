package com.eimsound.eimusic.ui.input

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClickTrackerTest {
    @Test
    fun sameRowWithinWindowIsDoubleClick() {
        val tracker = ClickTracker(400L)
        assertFalse(tracker.press(2, 1_000))
        assertTrue(tracker.press(2, 1_300))
    }

    @Test
    fun differentRowOrLatePressIsSingleClick() {
        val tracker = ClickTracker(400L)
        assertFalse(tracker.press(1, 1_000))
        assertFalse(tracker.press(2, 1_100))
        assertFalse(tracker.press(1, 1_600))
    }
}

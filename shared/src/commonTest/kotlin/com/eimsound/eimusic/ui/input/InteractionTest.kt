package com.eimsound.eimusic.ui.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MomentumScrollTest {
    private fun settle(scroll: MomentumScroll, max: Float = 100f): Int {
        var frames = 0
        while (scroll.advance(1f / 60f, max)) {
            frames++
            if (frames > 10_000) error("did not settle")
        }
        return frames
    }

    @Test
    fun oneWheelNotchTravelsThreeRowsAndStops() {
        val scroll = MomentumScroll()
        scroll.impulse(3)
        assertEquals(9f, scroll.velocity)
        settle(scroll)
        assertFalse(scroll.isMoving)
        assertTrue(scroll.position in 2.8f..3.0f, "position=${scroll.position}")
    }

    @Test
    fun upwardWheelClampsAtTop() {
        val scroll = MomentumScroll()
        scroll.jumpTo(0)
        scroll.impulse(-3)
        settle(scroll)
        assertEquals(0f, scroll.position)
        assertFalse(scroll.isMoving)
    }

    @Test
    fun wheelSpeedIsCapped() {
        val scroll = MomentumScroll()
        scroll.impulse(10_000)
        assertEquals(MomentumScroll.DEFAULT_MAX_SPEED, scroll.velocity)
    }

    @Test
    fun jumpToClearsVelocity() {
        val scroll = MomentumScroll()
        scroll.impulse(3)
        scroll.jumpTo(5)
        assertEquals(5f, scroll.position)
        assertFalse(scroll.isMoving)
    }
}

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

package com.eimsound.eimusic.version

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionTest {
    @Test
    fun parsesOptionalVPrefix() {
        assertEquals(listOf(1, 0, 1), parseVersion("v1.0.1"))
        assertEquals(listOf(1, 0, 1), parseVersion("V1.0.1"))
        assertEquals(listOf(1, 0, 1), parseVersion("1.0.1"))
    }

    @Test
    fun comparesMissingSegmentsAsZero() {
        assertEquals(0, compareVersions("1.0", "1.0.0"))
        assertTrue(compareVersions("1.0.1", "1.0") > 0)
    }

    @Test
    fun comparesNumericSegments() {
        assertTrue(compareVersions("1.0.0", "1.0.1") < 0)
        assertTrue(compareVersions("1.10.0", "1.9.9") > 0)
        assertEquals(0, compareVersions("v1.0.0", "1.0.0"))
    }
}

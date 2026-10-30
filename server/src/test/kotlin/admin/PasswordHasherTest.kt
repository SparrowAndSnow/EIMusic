package com.eimsound.eimusic.server.admin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {
    @Test
    fun hashesAndVerifiesPassword() {
        val hash = PasswordHasher.hash("correct horse battery staple")
        assertTrue(PasswordHasher.verify("correct horse battery staple", hash))
        assertFalse(PasswordHasher.verify("wrong password", hash))
        assertNotEquals(hash, PasswordHasher.hash("correct horse battery staple"))
    }

    @Test
    fun rejectsMalformedHashes() {
        assertFalse(PasswordHasher.verify("password", ""))
        assertFalse(PasswordHasher.verify("password", "not-a-hash"))
        assertFalse(PasswordHasher.verify("password", "pbkdf2-sha256\$bad\$salt\$hash"))
    }
}

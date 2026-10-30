package com.eimsound.eimusic.server.admin

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2-SHA256 密码哈希。
 *
 * 存储格式：pbkdf2-sha256$iterations$saltBase64$hashBase64
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2-sha256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val encoded = SecretKeyFactory.getInstance(ALGORITHM)
                .generateSecret(spec)
                .encoded
            "$PREFIX\$$ITERATIONS\$${base64(salt)}\$${base64(encoded)}"
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != PREFIX) return false

        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = decode(parts[2]) ?: return false
        val expected = decode(parts[3]) ?: return false

        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, expected.size * 8)
        return try {
            val actual = SecretKeyFactory.getInstance(ALGORITHM)
                .generateSecret(spec)
                .encoded
            MessageDigest.isEqual(actual, expected)
        } catch (_: Exception) {
            false
        } finally {
            spec.clearPassword()
        }
    }

    private fun base64(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun decode(value: String): ByteArray? =
        runCatching { Base64.getUrlDecoder().decode(value) }.getOrNull()
}

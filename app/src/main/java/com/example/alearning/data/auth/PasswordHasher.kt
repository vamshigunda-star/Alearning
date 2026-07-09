package com.example.alearning.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stateless utility for PBKDF2 password hashing and verification.
 *
 * Algorithm details (documented here as the single source of truth):
 *  - KDF:        PBKDF2WithHmacSHA256 (part of JVM standard library — no extra deps)
 *  - Iterations: 120,000 (NIST SP 800-132 minimum recommendation for SHA-256, 2023)
 *  - Salt:       16 bytes from [SecureRandom] (128-bit entropy)
 *  - Key length: 256 bits (32 bytes)
 *  - Comparison: [MessageDigest.isEqual] for constant-time byte comparison
 *
 * Storage format: salt and hash are stored as separate [ByteArray] columns
 * in [com.example.alearning.data.local.entities.auth.UserEntity] to avoid any
 * encoding overhead or parsing fragility.
 *
 * NEVER log passwords, salts, or derived keys.
 */
@Singleton
class PasswordHasher @Inject constructor() {

    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 120_000
        private const val SALT_LENGTH_BYTES = 16
        private const val KEY_LENGTH_BITS = 256
    }

    /**
     * Hashes [password] with a freshly generated random salt.
     *
     * @return A [Pair] of (salt, hash) — both must be stored together so
     *         [verify] can reproduce the same KDF invocation.
     */
    fun hash(password: String): Pair<ByteArray, ByteArray> {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt)
        return Pair(salt, hash)
    }

    /**
     * Returns `true` if [password] produces the same derived key as [expectedHash]
     * when computed with [salt].
     *
     * Uses [MessageDigest.isEqual] for a constant-time comparison that avoids
     * timing side-channels.
     */
    fun verify(password: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val candidate = derive(password, salt)
        return MessageDigest.isEqual(candidate, expectedHash)
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
